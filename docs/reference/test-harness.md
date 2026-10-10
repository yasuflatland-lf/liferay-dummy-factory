# Test harness reference

How the integration-test infrastructure works: the Gradle task graph, the DXP container, deployment, the Spock base class, and coverage. Commands for everyday use are in [`guides/testing.md`](../guides/testing.md). Decisions behind the design: [ADR-0001](../adr/0001-integration-test-architecture.md), [ADR-0008](../adr/0008-dxp-2026-migration.md).

## Gradle task graph

```
:integration-test:integrationTest
  ├── :modules:liferay-dummy-factory:jar
  ├── :startDockerContainer            (Workspace plugin)
  │     └── createDockerContainer → buildDockerImage → dockerDeploy
  │                                                     └── resolveLicenseFile
  ├── awaitLiferayReady                polls /c/portal/login for 200/302, up to 8 min
  └── installPlaywrightBrowsers        Playwright Java CLI: install chromium
  finalizedBy: :stopDockerContainer, jacocoIntegrationReport
```

- The default `test` task of `integration-test` is disabled; specs run only through `integrationTest` (JUnit Platform, 30-minute timeout, heap from `test.jvm.minHeap`/`maxHeap` in `gradle.properties`).
- `integrationTest` passes `liferay.host`, `liferay.http.port`, `liferay.gogo.port`, `liferay.jacoco.port`, `liferay.container.name`, `liferay.admin.email`, `liferay.admin.password` and `project.root.dir` as system properties.
- `integration-test/build.gradle` fails fast if the Workspace plugin did not register `:dockerDeploy` / `:createDockerContainer`.

### Incremental build trap

`integrationTest` does not declare the frontend toolchain (`package.json`, `yarn.lock`) as inputs. After a JS dependency change Gradle may report it `UP-TO-DATE` and replay the previous green result. For any frontend-toolchain change:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

A real run takes minutes; `BUILD SUCCESSFUL in` a few seconds means nothing ran.

## The container

| Property | Value |
|---|---|
| Image | `liferay/dxp:<liferay.workspace.product>` built by the Workspace plugin, with `configs/` and the JAR baked in |
| Name | `liferay-dummy-factory-liferay` (`${rootProject.name}-liferay`) |
| Ports (fixed) | 8080 HTTP, 11311 GoGo shell, 8000 JPDA, 6300 JaCoCo agent |
| Admin | `test@liferay.com`, password `test` until the first browser login changes it to `Test12345` ([why](dxp-runtime-config.md#5-use-the-current-admin-password)) |
| Removal | `autoRemove` is overridden to `false`: `stopDockerContainer` keeps the container and volume for post-mortem |
| JVM options | `LIFERAY_JVM_OPTS` (appended by the image's `setenv.sh`; `CATALINA_OPTS` would replace Liferay's own options) |

Ports are fixed by the Workspace plugin; another process on 8080 or 11311 makes `startDockerContainer` fail with "port is already allocated".

### License

DXP needs an activation key before the container serves requests. `resolveLicenseFile` (in `integration-test/build.gradle`) runs before `dockerDeploy` and writes `configs/local/deploy/activation-key.xml` from:

- `LIFERAY_DXP_LICENSE_FILE` — path to the XML (local), or
- `LIFERAY_DXP_LICENSE_BASE64` — base64 of the XML (CI secret).

With neither set, the build fails before Docker starts. The written key is git-ignored.

### State between runs

`stopDockerContainer` keeps the volume. `startDockerContainer` rebuilds the image layer from `configs/` and the JAR (Docker layer caching makes a JAR-only change fast; the first build takes several minutes). Database state can survive in the kept volume, which matters for anything seeded only on a fresh database (e.g. [SAP rows](dxp-runtime-config.md#service-access-policies-sap)). For a guaranteed clean state run `./gradlew removeDockerContainer` first — and always before trusting a surprising pass.

## Deployment and bundle activation

1. `dockerDeploy` copies the JAR into the image; on start it lands in Liferay's deploy directory and the auto-deploy scanner installs it.
2. `BaseLiferaySpec.ensureBundleActive()` (call it in `setupSpec()`) copies the freshly built JAR into the running container, then:
   - polls the container log every 2 s for up to 180 s until the last `STARTED liferay.dummy.factory_` line is newer than the last `STOPPED` line (a fresh container logs only `STARTED`; a reused one logs `STOPPED` then `STARTED`). Logs are read from 2 s before the copy to absorb clock skew; on timeout it fails with the last 30 log lines;
   - polls GoGo shell `lb` every 5 s for up to 5 minutes until the bundle is `Active`.
   It is `static synchronized` and runs once per test JVM (`bundleVerified`).
3. `LiferayContainer.deployJar(path)` (`docker cp` + `chown`) redeploys into a running container without rebuilding the image.

GoGo shell has no pipes: `lb | grep …` does not work, so the harness reads the whole `lb` output and filters in Groovy (`GogoShellClient`).

## `BaseLiferaySpec`

Every spec extends it. It provides:

| Member | Purpose |
|---|---|
| `liferay` | `LiferayContainer.getInstance()` — connection constants, `baseUrl`, `deployJar`, `logsSince` |
| `ensureBundleActive()` | See above |
| `loginAsAdmin(pw)` | Playwright login via API POST with CSRF token, handling the update-password interstitial |
| `jsonwsGet(path)` / `jsonwsPost(path, params)` | JSONWS with Basic Auth; `path` is the suffix after `/api/jsonws/` |
| `headlessGet(path)` / `headlessPost(path, json)` | Headless REST with Basic Auth |
| `basicAuthHeader()` | Uses `NEW_ADMIN_PASSWORD` |
| `PORTLET_ID` | `com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet` |
| `cleanupSpec()` | Dumps JaCoCo coverage for the spec (runs even when a subclass defines its own `cleanupSpec()`) |

All harness HTTP requests send `Accept-Encoding: identity`: `HttpURLConnection` does not decompress gzip, and compressed bodies otherwise break JSON parsing.

Helpers under `it/util/`: `LdfResourceClient` (calls `/ldf/*` resource commands through a logged-in browser session), `WorkflowHttpClient` (`/o/ldf-workflow`), `GogoShellClient`, `JsonwsSetupHelper`, `PlaywrightLifecycle`.

## Coverage

### Java unit tests

`./gradlew :modules:liferay-dummy-factory:test` is finalized by `jacocoTestReport`:

- `modules/liferay-dummy-factory/build/reports/jacoco/test/html/index.html`
- `modules/liferay-dummy-factory/build/reports/jacoco/test/jacocoTestReport.xml`

### Integration tests

Coverage is collected inside the Liferay JVM, not the test JVM:

- `createDockerContainer` mounts the JaCoCo runtime agent read-only at `/opt/liferay/jacocoagent.jar` and sets `LIFERAY_JVM_OPTS=-javaagent:…=output=tcpserver,address=0.0.0.0,port=6300`.
- Each spec's `cleanupSpec()` dumps a `.exec` file into `integration-test/build/jacoco/`; `jacocoIntegrationReport` merges them into `integration-test/build/reports/jacoco/integration/` (`index.html`, `jacoco.xml`).
- `./gradlew :integration-test:jacocoIntegrationReport` regenerates the report from existing `.exec` files.

Pitfalls that each cost a debugging session:

- **Harness instrumentation.** Applying the `jacoco` plugin instruments every `Test` task; `integrationTest` sets `jacoco { enabled = false }` so only container-side data is collected.
- **Port 6300 needs `exposePorts` and a binding.** The base image does not `EXPOSE` 6300, and Docker silently drops port bindings for unexposed ports. The binding has no host-IP prefix so it is published on both IPv4 and IPv6 — on GitHub Actions Java 21 resolves `localhost` to `::1`, and a `127.0.0.1`-only binding gives "Connection refused". The JaCoCo agent is unauthenticated, so this exposes it on every host interface while the container runs — fine on CI runners and a dev machine, not on a shared network.
- **Binds are fixed at configuration time.** bmuschko's `hostConfig.binds` is an `@Input`, so it must be set in the configure block, not in `doFirst` ("value … is final").
- **Agent JAR name.** In the Gradle cache the agent is `org.jacoco.agent-<ver>-runtime.jar`, so the lookup matches `jacocoagent` *or* (`org.jacoco.agent` and `runtime`).
- **Silent skip.** `jacocoIntegrationReport` has `onlyIf` "some `.exec` exists"; when the agent is unreachable it is skipped without error, and Codecov (`fail_ci_if_error: false`) stays green. CI therefore fails explicitly if `jacoco.xml` is missing.
- **Evaluation order.** The report reads the module's `sourceSets.main`, so `integration-test/build.gradle` starts with `evaluationDependsOn(':modules:liferay-dummy-factory')`.

## CI

| Workflow | Trigger | Runs |
|---|---|---|
| `.github/workflows/unit-test.yml` | push to `master`, every PR | Java unit tests, Vitest with coverage, docs check, Codecov upload |
| `.github/workflows/integration-test.yml` | push / PR to `master` | Full `integrationTest` against DXP with the `LIFERAY_DXP_LICENSE_BASE64` secret; uploads coverage and, on failure, container logs |

On GitHub runners the deploy directories bind-mounted by the Workspace plugin are made world-writable before the tests, because the runner (UID 1001) and the container's `liferay` user (UID 1000) both write there.
