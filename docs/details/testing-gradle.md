# Gradle test execution — concrete details

L3 detail. Source of truth for Gradle task wiring, deploy verification, the incremental build trap, and JaCoCo coverage. Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

This file reflects the DXP 2026.Q1.3-LTS workspace-native Docker flow. The Testcontainers-based approach used for CE 7.4 GA132 has been removed. See `docs/ADR/adr-0008-dxp-2026-migration.md` for the migration decisions.

## License requirement (DXP 2026)

DXP 2026 requires a valid activation key before the container can serve requests. Set one of these environment variables before running any Gradle integration test task:

```bash
# Local development (file path):
export LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml

# CI (base64-encoded XML blob, set as a GitHub Actions secret):
# LIFERAY_DXP_LICENSE_BASE64=<base64 of activation-key.xml>
```

The `resolveLicenseFile` Gradle task reads these variables and writes the key to `configs/local/deploy/activation-key.xml` before `dockerDeploy` runs. If both variables are absent the build fails immediately with a clear error message. The activation key file is `.gitignore`d.

## Running tests

```bash
# Build the module JAR first (required by integration tests)
./gradlew :modules:liferay-dummy-factory:jar

# Run integration tests (starts DXP 2026 container, runs Spock specs, stops container)
./gradlew :integration-test:integrationTest

# Single spec (image must already be running from a prior startDockerContainer)
./gradlew :integration-test:integrationTest \
    --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

- The module build depends on `release.dxp.api` (DXP 2026 BOM). Do not mix individual API artifacts alongside this dependency — version skew causes runtime failures.
- The default `test` task is **disabled** (`enabled = false`). All integration tests run exclusively via the `integrationTest` task.
- The `integrationTest` task automatically depends on `:modules:liferay-dummy-factory:jar`, so a standalone `./gradlew :integration-test:integrationTest` will build the JAR first.
- JVM args: `-Xms4g -Xmx4g`.
- Test logging outputs `passed`, `skipped`, `failed`, `standardOut`, and `standardError`.

## Workspace-native Docker lifecycle

The workspace plugin 16.0.5 registers these root-project tasks:

```
startDockerContainer      # Chains: resolveLicenseFile → dockerDeploy → buildDockerImage
                          #         → createDockerContainer → start
stopDockerContainer       # Stops the container; volume is preserved (autoRemove=false override in integration-test/build.gradle)
removeDockerContainer     # Stops + removes the container and volume (hard clean)
```

### Task graph

```
integrationTest
  ├── :modules:liferay-dummy-factory:jar
  ├── :startDockerContainer
  │     └── createDockerContainer
  │           └── buildDockerImage
  │                 └── dockerDeploy (copies configs/ overlays, including module JAR)
  │                       └── resolveLicenseFile (env var → configs/local/deploy/activation-key.xml)
  ├── awaitLiferayReady (polls http://localhost:8080/c/portal/login, 8-minute timeout)
  └── installPlaywrightBrowsers
  finalizedBy: stopDockerContainer, jacocoIntegrationReport
```

### Local development workflow

For repeated spec runs without rebuilding the image each time:

```bash
# Start the container once (builds image, resolves license, starts DXP)
./gradlew startDockerContainer

# Run a single spec without restarting the container
./gradlew :integration-test:integrationTest \
    --tests "com.liferay.support.tools.it.spec.DeploymentSpec"

# Stop when done
./gradlew stopDockerContainer

# Hard clean (force volume recreation on next start)
./gradlew removeDockerContainer
```

### `startDockerContainer` fails with "Status 304"

If `:startDockerContainer` fails immediately with `Status 304:` and an empty body, the container is already running. The bmuschko Docker plugin's start operation returns HTTP 304 Not Modified for this case rather than a meaningful error message.

Non-destructive fix (preserves volume state):

```bash
./gradlew stopDockerContainer
./gradlew :integration-test:integrationTest
```

Destructive fix (hard reset, drops volume state — use when container state is corrupt):

```bash
./gradlew removeDockerContainer
./gradlew :integration-test:integrationTest
```

A common cause is a prior `:startDockerContainer` that failed mid-way (missing license, port conflict, out-of-memory OOM kill) after Docker had already started the container. Check the container state before deciding which fix to apply:

```bash
docker ps -a --filter name=liferay
```

If `Status` is `Up (healthy)` and Gradle insists it is not up, stop first. If status is `Exited` with a non-zero code, inspect logs with `docker logs <container-name>` before restarting.

### `dockerDeploy` incremental behavior

`dockerDeploy` copies `configs/` overlays into the Docker build context before the image is built. Docker layer caching applies: if the `configs/` content is unchanged, the portal layer is reused and only the changed layers (e.g., a new JAR in `deploy/`) are rebuilt. In practice, a JAR-only change takes tens of seconds rather than minutes. The first build from scratch (fresh image pull + layer construction) takes 5–10 minutes depending on network speed.

## Deploy verification

1. The module JAR is baked into the Docker image via `dockerDeploy` (copied to `configs/<env>/deploy/`). On container start, the workspace plugin copies it to `/mnt/liferay/deploy/`, and Liferay's AutoDeployScanner picks it up.
2. Bundle activation is verified via GoGo Shell: the `lb` output must contain `Liferay Dummy Factory` with state `Active`.
3. The `ensureBundleActive()` method in `BaseLiferaySpec` polls GoGo Shell every 5 seconds for up to 5 minutes until the bundle is active. It is `synchronized` and runs only once per test suite.
4. For mid-session re-deployment (without rebuilding the image), `LiferayContainer.deployJar(path)` uses `docker cp` + `docker exec chown` to place the JAR in the running container's deploy directory.

## Gradle Incremental Build Trap

`:integration-test:integrationTest` does **not** declare `package.json` as an input. Changing a JavaScript dependency (e.g. bumping React, swapping Jest for Vitest) does not invalidate the `integrationTest` task, so Gradle marks it `UP-TO-DATE` and **replays the previous run's result** without executing anything. A regression introduced in the JS toolchain will appear as a green build.

For any change touching the frontend toolchain, the only trustworthy verification is:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

Tell real runs from cached replays by the elapsed time on the `BUILD SUCCESSFUL in Xs` line. A real run is on the order of minutes (container startup + Playwright); a cached replay is single-digit seconds. If the build completes in single-digit seconds, assume the tests did not actually run and re-invoke with the `clean` tasks above.

## Coverage (JaCoCo)

### Host-JVM unit tests

Unit tests under `modules/liferay-dummy-factory/src/test/java` are measured by JaCoCo. The report is generated automatically after `test` via `finalizedBy`.

```bash
./gradlew :modules:liferay-dummy-factory:test
```

Report locations:

- HTML: `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- XML:  `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

### Integration tests — Liferay container JVM

Coverage is collected from the Liferay container JVM, not the integration test harness. The JaCoCo agent is injected into the container via `LIFERAY_JVM_OPTS` (not `CATALINA_OPTS`) in tcpserver mode on port 6300. At the end of each spec, `BaseLiferaySpec.cleanupSpec()` dumps a per-spec `.exec` file to `integration-test/build/jacoco/`. The `jacocoIntegrationReport` task merges all exec files and generates the combined report.

To run integration tests and generate the report:

```bash
./gradlew :integration-test:integrationTest
# jacocoIntegrationReport runs automatically as finalizedBy
```

To regenerate the report from existing exec files without re-running tests:

```bash
./gradlew :integration-test:jacocoIntegrationReport
```

Report locations:

- HTML: `integration-test/build/reports/jacoco/integration/index.html`
- XML:  `integration-test/build/reports/jacoco/integration/jacoco.xml`

### JaCoCo integration pitfalls

**Gradle JaCoCo plugin instruments ALL Test tasks.** Applying `id 'jacoco'` to the integration-test project causes Gradle to auto-instrument every `Test` task, including `integrationTest`. This produces a spurious `integrationTest.exec` from the harness JVM rather than from the Liferay container. Fix: add `jacoco { enabled = false }` inside the `integrationTest` task block to disable harness-side instrumentation and keep coverage collection container-side only.

**The `org.jacoco.agent:runtime` jar has a non-obvious classpath filename.** The Maven artifact `org.jacoco:org.jacoco.agent:0.8.14:runtime` is stored in the Gradle cache as `org.jacoco.agent-0.8.14-runtime.jar`. Searching for the string `jacocoagent` alone will miss it. The correct predicate is: the path contains `jacocoagent` OR (contains `org.jacoco.agent` AND contains `runtime`). The agent is mounted into the container via a Docker volume bind (`:ro`) to `/opt/liferay/jacocoagent.jar` at `createDockerContainer` time.

**Spock `cleanupSpec()` inheritance in Spock 2.x.** A `cleanupSpec()` defined in an abstract base spec IS invoked even when the concrete subclass defines its own `cleanupSpec()`. Spock's `PlatformSpecRunner.doRunCleanupSpec` chains the hierarchy. `BaseLiferaySpec.cleanupSpec()` is therefore guaranteed to dump JaCoCo coverage at the end of every spec, regardless of whether the subclass also defines `cleanupSpec()`. No explicit `super.cleanupSpec()` call is needed in subclasses.

**`LIFERAY_JVM_OPTS` is the correct env var for JVM option injection.** The DXP 2026 Docker image's `setenv.sh` appends `$LIFERAY_JVM_OPTS` to the JVM startup command. `CATALINA_OPTS` would replace Liferay's built-in JVM options rather than supplement them. The JaCoCo agent is injected via `createDockerContainer { withEnvVar('LIFERAY_JVM_OPTS', '-javaagent:/opt/liferay/jacocoagent.jar=...') }`.

**bmuschko `DockerCreateContainer.hostConfig.binds` is `@Input @Optional` — MUST be set at configuration time.** The bmuschko Docker plugin annotates `hostConfig.binds` (a `MapProperty<String, String>`) as `@Input`, so Gradle finalizes the property before task actions run. Calling `hostConfig.binds.put(...)` inside a `doFirst` block fails at runtime with `"The value for property 'binds' is final and cannot be changed any further."` The bind mount for the JaCoCo agent JAR must be set inside the `createDockerContainer { ... }` configure block (configuration time), not deferred to `doFirst`.

**Port 6300 is NOT in the workspace plugin 16.0.5 default `portBindings`.** The workspace plugin's default bindings are `8000:8000`, `8080:8080`, and `11311:11311`. Port 6300 (JaCoCo tcpserver) is absent. Without an explicit `hostConfig.portBindings.add('127.0.0.1:6300:6300')` in `createDockerContainer`, `ExecDumpClient` on the host cannot reach the agent and every spec's coverage dump silently fails (no error, no exception — the client times out and the `.exec` file is never written).

**JaCoCo tcpserver is unauthenticated — bind the host side to loopback.** The JaCoCo `tcpserver` mode has no authentication mechanism. On a shared CI runner or a developer machine with other processes on the LAN, binding to `0.0.0.0:6300` exposes the agent to all network interfaces. Use `127.0.0.1:6300:6300` (loopback-only host binding) in `hostConfig.portBindings` to restrict access to the local process only.

**`jacocoIntegrationReport` is silently SKIPPED when no `.exec` files exist.** The task has an `onlyIf { !fileTree('build/jacoco').isEmpty() }` guard. When `ExecDumpClient` cannot connect (e.g., port 6300 was not published), no `.exec` files are written, the guard fires, and the task is skipped without any error. Codecov's `fail_ci_if_error: false` then uploads nothing and still reports green. Add an explicit file-existence check in CI (`[ ! -s "integration-test/build/reports/jacoco/integration/jacoco.xml" ] && exit 1`) as a diagnostic signal that coverage collection failed.

**`evaluationDependsOn(':modules:X')` is required when `jacocoIntegrationReport` references cross-project `sourceSets`.** `jacocoIntegrationReport` reads `project(':modules:liferay-dummy-factory').sourceSets.main.*`. Gradle evaluates projects in settings.gradle `include` order by default; if `integration-test` is evaluated before `modules:liferay-dummy-factory`, `sourceSets.main` is not yet populated and the task configuration throws `NullPointerException`. Fix: add `evaluationDependsOn(':modules:liferay-dummy-factory')` at the top of `integration-test/build.gradle`.

## Release-only task: `copyJarToLatest`

`./gradlew :modules:liferay-dummy-factory:copyJarToLatest` publishes the built JAR to `latest/liferay-dummy-factory.jar` (the distribution path linked from `README.md` §Usage).

The `jar.finalizedBy copyJarToLatest` auto-hook was removed in commit `ceda5e3` so ordinary dev-loop `jar` runs do not churn `latest/`. Run this task **only** when cutting a release, and commit the updated `latest/` JAR as part of the release PR. Do not re-add the auto-hook — every dev build would otherwise produce a spurious `latest/` diff.
