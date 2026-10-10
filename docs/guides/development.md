# Development setup

Everything needed to build the bundle and run it in a local DXP container. Running the test suites is covered in [testing.md](testing.md).

## Prerequisites

| Tool | Notes |
|---|---|
| JDK 21, Node, Yarn | Versions are in `mise.toml`; `mise install` sets them up. Gradle itself comes from the wrapper. |
| Docker | Used by the Workspace plugin for the DXP container. Ports 8080, 11311, 8000 and 6300 must be free. |
| DXP activation key | Required for the container and for integration tests. |

If the Gradle daemon picks the wrong JDK, set `org.gradle.java.home` in your **user** `~/.gradle/gradle.properties` (never in the repository's `gradle.properties`).

Provide the activation key through an environment variable:

```bash
export LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml      # local
# or
export LIFERAY_DXP_LICENSE_BASE64="$(base64 -w0 activation-key.xml)"   # CI style
```

## Build

```bash
./gradlew :modules:liferay-dummy-factory:jar
```

The JAR task also runs the frontend build (`yarn build` → `modules/liferay-dummy-factory/scripts/build.mjs`). The output is `modules/liferay-dummy-factory/build/libs/`.

## Run DXP locally

```bash
./gradlew startDockerContainer     # build the image (configs/ + JAR) and start DXP on :8080
./gradlew stopDockerContainer      # stop; container and volume are kept
./gradlew removeDockerContainer    # remove container and volume (clean state next start)
```

Then open <http://localhost:8080>, sign in as `test@liferay.com` / `test` (the first browser login asks for a new password; the test suite uses `Test12345`), and open the portlet as described in the [installation guide](installation.md#3-open-it).

The first image build takes several minutes; later builds reuse cached layers. Startup itself takes a few minutes — wait for `/c/portal/login` to answer.

### Edit → deploy loop

- **Java / JSP / JS change:** rebuild the JAR and copy it into the running container's deploy directory:

  ```bash
  ./gradlew :modules:liferay-dummy-factory:jar
  JAR=$(ls modules/liferay-dummy-factory/build/libs/*.jar)
  docker cp "$JAR" liferay-dummy-factory-liferay:/tmp/
  docker exec -u 0 liferay-dummy-factory-liferay bash -c \
    "cp /tmp/$(basename "$JAR") /opt/liferay/deploy/ && chown liferay:liferay /opt/liferay/deploy/$(basename "$JAR")"
  ```

  The `chown` matters: a root-owned file in the deploy directory cannot be processed by the auto-deploy scanner. `LiferayContainer.deployJar` does the same from tests. There is no hot reload for the frontend.
- **`configs/` change:** the files are baked into the image, so restart with `stopDockerContainer` + `startDockerContainer`; database-seeded settings may need `removeDockerContainer`.

Check the bundle state:

```bash
docker exec liferay-dummy-factory-liferay bash -c "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"
```

`Active` is good. Anything else: [troubleshooting](troubleshooting.md).

## Format

```bash
cd modules/liferay-dummy-factory
yarn format          # write
yarn checkFormat     # verify
```

Java follows Liferay conventions (tabs, `_` prefix for private members); see [`.claude/rules/backend-java.md`](../../.claude/rules/backend-java.md).

## Where to go next

- How the code is organized: [architecture overview](../architecture/overview.md)
- Adding a new entity type end to end: [adding-an-entity.md](adding-an-entity.md)
- Contribution rules and PR checklist: [CONTRIBUTING.md](../../CONTRIBUTING.md)
