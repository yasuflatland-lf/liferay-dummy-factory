# Running the tests

Three suites, from fastest to slowest. How the harness works internally is in the [test harness reference](../reference/test-harness.md); how to write tests is in [`.claude/rules/tests.md`](../../.claude/rules/tests.md).

| Suite | Command | Needs |
|---|---|---|
| Java unit (JUnit 5) | `./gradlew :modules:liferay-dummy-factory:test` | JDK |
| Frontend unit (Vitest) | `cd modules/liferay-dummy-factory && yarn test` | Node |
| Integration / E2E (Spock + Playwright) | `./gradlew :integration-test:integrationTest` | Docker + DXP activation key |

## Unit tests

```bash
./gradlew :modules:liferay-dummy-factory:test               # + JaCoCo report in build/reports/jacoco/test/
cd modules/liferay-dummy-factory
yarn test                                                   # Vitest once
yarn test:watch                                             # Vitest watch mode
yarn test:coverage                                          # + coverage/lcov.info
```

## Integration tests

```bash
export LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml
./gradlew :integration-test:integrationTest
```

This builds the JAR, builds and starts the DXP container, waits up to 8 minutes for it, installs Chromium, runs every spec, then stops the container and writes the coverage report to `integration-test/build/reports/jacoco/integration/`.

One spec:

```bash
./gradlew :integration-test:integrationTest \
    --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

Faster iteration: start the container once with `./gradlew startDockerContainer`, then run single specs repeatedly. Use `--info` for verbose output; `docker logs -f liferay-dummy-factory-liferay` for the portal log.

Compile the specs without a container (note the task name — `compileGroovy` has no sources and always passes):

```bash
./gradlew :integration-test:compileTestGroovy
```

### Trust the result

- **Clean state.** The container volume survives `stopDockerContainer`. Before trusting an unexpected pass (or debugging an unexpected failure), run `./gradlew removeDockerContainer`.
- **Not a replay.** After any frontend-toolchain change (`package.json`, `yarn.lock`), clean first; otherwise Gradle can replay an old green result:

  ```bash
  ./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
  ./gradlew :integration-test:integrationTest
  ```

  A real run takes minutes. `BUILD SUCCESSFUL in 5s` means nothing ran.

## CI

`unit-test.yml` runs on every PR (Java + Vitest + docs check); `integration-test.yml` runs on PRs to `master` with the `LIFERAY_DXP_LICENSE_BASE64` secret. Both upload coverage to Codecov. On integration failure, download the `integration-test-report` artifact: it contains the test reports and the container log.
