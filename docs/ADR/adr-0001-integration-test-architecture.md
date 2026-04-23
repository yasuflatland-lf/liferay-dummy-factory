# ADR-0001: Integration Test Architecture for Liferay Portal CE

## Status

Accepted (original 2026-04-09) — container strategy section superseded in part by ADR-0008 (2026-04-18).

See `docs/ADR/adr-0008-dxp-2026-migration.md` for the DXP 2026 migration decisions, including the replacement of Testcontainers with workspace-native Docker (Liferay workspace plugin 16.0.5).

## Date

2026-04-09

## Context

The liferay-dummy-factory project needs to build E2E integration tests for the portlet (MVCPortlet + React).

**Original context (CE 7.4 GA132)**: Integration tests used Testcontainers 2.0.4 against `liferay/portal:7.4.3.132-ga132`.

**Current context (DXP 2026.Q1.3-LTS)**: Testcontainers has been removed entirely. Container lifecycle is managed by workspace-native Docker tasks from `com.liferay.gradle.plugins.workspace:16.0.5` (`startDockerContainer` / `stopDockerContainer` / `removeDockerContainer`). The container image is `liferay/dxp:2026.q1.3-lts`. See ADR-0008 for rationale.

### Constraints

- **Target**: Liferay DXP 2026.Q1.3-LTS (as of 2026-04-18; formerly CE 7.4 GA132)
- **Runtime environment**: WSL2 + Docker Desktop 29.x
- **Build**: Gradle 8.5 + Liferay Workspace Plugin 16.0.5
- **Test scope**: OSGi bundle deployment verification, bundle state validation via GoGo Shell, and browser E2E tests via Playwright

## Decision

### 1. Test Framework Configuration

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Test framework | Spock 2.4 + Groovy 5.0.4 | Groovy's concise syntax, test ordering via `@Stepwise`, Power Assert |
| Container management | Workspace-native Docker (plugin 16.0.5) | Replaces Testcontainers 2.0.4. `startDockerContainer` / `stopDockerContainer` / `removeDockerContainer` tasks manage lifecycle natively. Fixed ports 8080/11311/8000. See ADR-0008 D1-D8. |
| Browser tests | Playwright Java 1.59.0 (Chromium only) | Same technology stack as the official Liferay tests. Installing only Chromium reduces download time |
| GoGo Shell communication | Apache Commons Net (Telnet) | Used to connect to the Liferay OSGi console |

### 2. Login Method: API POST (with CSRF Token)

**Decision**: Adopt the same approach as the official Liferay Playwright tests (`performLoginViaApi`).

```
1. page.navigate("/") to establish a session
2. page.evaluate("() => Liferay.authToken") to obtain the CSRF token
3. page.request().post("/c/portal/login") with the CSRF token
4. page.navigate("/") to reload
```

**Rejected alternatives**:
- **Form-based login**: After login, redirecting to `/web/guest` causes the session to not carry over. Conditional handling for the password change page is complex.
- **Basic authentication**: Disabled by default in Liferay CE.

### 3. Password Policy Handling

**Decision (DXP 2026)**: Suppressed entirely via `configs/common/portal-ext.properties`.

`company.security.update.password.required=false` and `passwords.default.policy.change.required=false` are set in `portal-ext.properties` (baked into the image via `dockerDeploy`). The `PASSWORDRESET` flag is never set, so the admin account (`test@liferay.com` / `test`) is usable immediately without any password change flow. `BaseLiferaySpec` needs no special handling. See ADR-0008 D2.

**Original CE 7.4 approach (historical)**: Used the `LIFERAY_PASSWORDS_DEFAULT_POLICY_CHANGE_REQUIRED=false` environment variable plus a password-trial fallback (`test` then `Test12345`). This approach was necessary because CE Docker images use a pre-built database that env vars can patch at startup, while `portal-ext.properties` only affects non-password settings in that environment. This complexity is eliminated in DXP 2026.

**Rejected alternatives (CE 7.4 era)**:
- **DB update via GoGo Shell**: Direct SQL execution is not possible from the OSGi console.
- **Groovy script execution**: `groovy:exec` in GoGo Shell is not available in the CE Docker image.

### 4. JAR Deployment Method: Copy via /tmp + chown

**Decision**: Place the file in `/tmp` using `copyFileToContainer`, then use `execInContainer` to `cp` + `chown liferay:liferay` and move it to `/opt/liferay/deploy/`.

**Rationale**: `copyFileToContainer` creates files owned by root, but Liferay's AutoDeployScanner runs as the liferay user (uid=1000). Copying directly to `/deploy/` causes an `Unable to write` error.

### 5. GoGo Shell Bundle Verification: Full Output Retrieval + Java-Side Filtering

**Decision**: Retrieve the full output of the `lb` command (approximately 1394 lines) and filter for lines containing `Liferay Dummy Factory` on the Java/Groovy side.

**Rationale**: GoGo Shell is an OSGi console and does not support Unix shell pipes (`|`) or the `grep` command. Running `lb | grep dummy.factory` simply causes the `grep` command to return `false`.

### 6. Verification Strategy: JSONWS First, Playwright Only for UI

**Decision**: Post-condition assertions (did the entity actually get created / updated / deleted?) go through Liferay JSONWS (`/api/jsonws/...`) with Basic Auth. Playwright is reserved for behavior that is genuinely UI-specific (rendering, client-side validation, navigation flows).

**Rationale**: JSONWS is faster and deterministic, and it does not depend on Control Panel rendering or portlet UI state. Relying on Playwright for data assertions couples the test outcome to transient UI layout, and the Elasticsearch-backed headless REST endpoints (`/o/headless-admin-user/...`) have observable indexing lag that makes post-create reads non-deterministic; JSONWS goes directly through the service layer and avoids both issues.

### 7. Creator Services Declare `throws Throwable`

**Decision**: The public `create(...)` method on every `*Creator` service in `com.liferay.support.tools.service` declares `throws Throwable`, and the `*ResourceCommand` callers use `catch (Throwable t)`.

**Rationale**: `TransactionInvokerUtil.invoke(TransactionConfig, Callable)` is declared `throws Throwable` — its `Callable`-shaped lambda lets the transaction machinery surface both checked exceptions and `Error`s. Adding a `catch (Throwable) { throw new Exception(t); }` bridge inside each Creator would let the public signature return to `throws Exception`, but it would also (a) flatten the distinction between `PortalException` subtypes that the ResourceCommand can decide to render as user errors, and (b) re-wrap the original throwable, obscuring the root cause in logs.

**Trade-off accepted**: `catch (Throwable)` at the ResourceCommand layer will also catch `Error` subtypes (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`). In the portlet request path this is acceptable: the JVM's error state is reported to the user as a failed action rather than silently killing the worker thread, and the portlet container will continue to serve other requests. If this ever becomes a problem, the bridge-and-rethrow pattern can be added in the Creators without changing the ResourceCommand contract.

### 8. Container Configuration

**DXP 2026 (current)**: Container lifecycle is managed by workspace-native Gradle tasks. Configuration is baked into the Docker image via `configs/` overlays during `dockerDeploy`. No Testcontainers API calls remain.

Key workspace plugin container behaviors:
- Container name: `${project.name}-liferay` (e.g. `liferay-dummy-factory-liferay`)
- `autoRemove=false` via explicit override in `integration-test/build.gradle` (workspace plugin 16.0.5 defaults to `true` at `RootProjectConfigurator.java:540`). Container persists after `stopDockerContainer` for post-mortem inspection.
- `LIFERAY_JVM_OPTS` is injected via `createDockerContainer { withEnvVar(...) }` (JaCoCo agent, JPDA).
- Portal configuration lands in `configs/common/portal-ext.properties` and is merged at image build time.
- Activation key is copied to `configs/local/deploy/` by the `resolveLicenseFile` task before `dockerDeploy`.

**CE 7.4 original (historical, removed)**:
```groovy
withReuse(false)                   // Always start a fresh container to prevent state leakage
withCopyToContainer(...)           // Place portal-ext.properties
withEnv([                          // Environment variables
    'LIFERAY_SETUP_WIZARD_ENABLED': 'false',
    'LIFERAY_TERMS_OF_USE_REQUIRED': 'false',
    'LIFERAY_USERS_REMINDER_QUERY_ENABLED': 'false',
])
```

The intent of `withReuse(false)` (preventing state leakage between test runs) is preserved in DXP 2026 by `autoRemove=false` + `stopDockerContainer` (container is stopped and volume kept, but not reused by the next `startDockerContainer` invocation which creates a fresh container from the image). For a guaranteed clean state, run `./gradlew removeDockerContainer` before the next `startDockerContainer`.

## Consequences

### Positive

- Login method follows the official Liferay Playwright test patterns
- Workspace-native Docker guarantees a clean Liferay state for every test run (each `startDockerContainer` starts from the baked image). Entities or password changes from a previous run cannot leak across runs.
- Installing only Chromium reduces download time
- Testcontainers dependency removed — no more Testcontainers version compatibility concerns with Docker Engine

### Negative

- **No Global Menu in CE GA132 (historical note)**: The DXP-only Global Menu did not exist in CE GA132. Resolved by using direct URL access with `p_p_state=maximized`. In DXP 2026 the Global Menu exists but direct URL access is still used for speed.
- Each test run pays the full ~8 minute container startup cost. This is an intentional trade-off to preserve test isolation.
- Fixed ports (8080/11311/8000) conflict with any other Liferay process running on the developer machine. Developers must stop conflicting processes before running tests.

### Resolved Questions

1. **PanelApp navigation on CE GA132**: Resolved by using direct URL access with `p_p_state=maximized` query parameter. The URL pattern `/group/control_panel/manage?p_p_id=<portlet_id>&p_p_lifecycle=0&p_p_state=maximized` renders the portlet directly without needing the Product Menu sidebar or Global Menu.

### Open Questions

1. **Playwright version**: Currently using 1.59.0. Consider updating as needed while maintaining compatibility with the official Liferay tests.

## References

- Liferay Portal source: `/home/yasuflatland/tmp/liferay-portal`
- Official Liferay Playwright tests: `modules/test/playwright/`
  - `utils/performLogin.ts` -- API login pattern
  - `helpers/ApiHelpers.ts` -- CSRF token retrieval
  - `pages/product-navigation-applications-menu/GlobalMenuPage.ts` -- Global Menu (DXP)
  - `utils/productMenu.ts` -- Product Menu
  - `env/portal-ext.properties` -- Test properties
- Testcontainers source: `/home/yasuflatland/tmp/testcontainers-java`
- Detailed implementation plan: `.claude/plan/integrationtest.md`

## Release Cadence Caveat

Playwright Java (`com.microsoft.playwright:playwright` on Maven Central) and Playwright Node/CLI (`playwright` on npm) follow independent release cadences. A given `1.x.y` tag does not necessarily exist on both channels. For example, as of 2026-04 the latest stable on npm is `1.59.1`, while the latest on Maven Central is `1.59.0`; `1.59.1` and `1.60.0` have not yet been published to Maven Central.

The Playwright project recommends keeping the client library and the driver pinned to the same version because the wire protocol is only guaranteed to be compatible within a single release. Mixing a Java client with a different Node driver version can fail at runtime with opaque protocol errors.

When bumping the Playwright version, always confirm the target POM actually exists on Maven Central before changing any build file. A HEAD against the pom URL is the most reliable check:

```
curl -s -o /dev/null -w "%{http_code}" https://repo.maven.apache.org/maven2/com/microsoft/playwright/playwright/<version>/playwright-<version>.pom
```

A `200` response means the artifact is available; anything else (typically `404`) means the version is npm-only and must not be adopted on the Java side yet. The `test.playwright.version` property in `gradle.properties` and the `npx playwright@<version>` invocation in the CI workflow must always reference the same version so the Java client and the Node driver stay in lockstep.
