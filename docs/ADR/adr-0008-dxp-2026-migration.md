# ADR-0008: Migrate to Liferay DXP 2026.Q1.3-LTS

## Status: Accepted (2026-04-18)

## Context

Liferay Portal CE 7.4 GA132 reached end-of-life. The project must migrate to Liferay DXP 2026.Q1.3-LTS (`liferay/dxp:2026.q1.3-lts`). A first migration attempt was made on branch `feature/migrate_to_dxp`, which was closed without merging. Part 2 starts cleanly from `master`.

Key changes imposed by the new platform:

- **Portlet API**: DXP 2026 requires `jakarta.portlet` 4.0 (`jakarta.portlet.*` imports, `jakarta.portlet.version=4.0` component properties). The `javax.portlet` namespace is no longer exported.
- **Servlet API**: DXP 2026 OSGi runtime does not export `javax.servlet` or `javax.servlet.http`. Bundles that try to import them at startup will be UNSATISFIED.
- **JSP taglib URI**: DXP 2026's `Provide-Capability` only advertises `http://xmlns.jcp.org/portlet_3_0`. Switching the JSP `<%@ taglib %>` URI to `jakarta.tags.portlet` causes bundle resolution failure. The taglib URI stays on JCP.
- **JSONWS base path**: DXP 2026.Q1.3-LTS continues to expose JSON Web Services at `/api/jsonws/`. Earlier migration notes referred to `/portal/api/jsonws/`, but that path is not registered in this release and returns 404. See `docs/details/api-liferay-dxp2026.md` §14.
- **BasicAuth AuthVerifier configuration policy**: DXP 2026 marks `BasicAuthHeaderAuthVerifierPipelineConfigurator` with `configuration-policy="require"`. Without an explicit OSGi config entry, BasicAuth credentials are silently ignored and every JSONWS call falls through to Guest. Deploy a `.config` at `configs/common/osgi/configs/com.liferay.portal.security.auth.verifier.internal.basic.auth.header.configuration.BasicAuthHeaderAuthVerifierConfiguration-default.config` with `enabled=B"true"`, `urlsIncludes="/api/*,/o/*,/xmlrpc/*"` (comma-separated String, NOT array literal — see `docs/details/dxp-2026-gotchas.md` §2), and `forceBasicAuth=B"true"` for visibility on wrong-password failures.
- **BasicAuth login flow hardened off**: DXP 2026's image bakes `portal-liferay-online-config.properties` with `BasicAuthHeaderSupportConfiguration_enabled=B"false"`. That file is processed AFTER `portal-ext.properties`, so `configuration.override.*` in user portal-ext loses the priority race. Even an OSGi `.config` is overwritten by the online-config on every restart. The only durable override is a runtime `ConfigurationAdmin.update(...)` call, implemented as the test-only OSGi component `BasicAuthTestSetup`. Detail in `docs/details/dxp-2026-gotchas.md` §11.
- **JSON Web Service Servlet has its own AuthVerifierFilter**: `/api/jsonws/*` is served by a separate filter that reads `jsonws.servlet.*` portal properties, NOT the OSGi pipeline configured by the BasicAuth `.config` above. BasicAuth must be re-registered there via portal-ext entries. Detail in `docs/details/dxp-2026-gotchas.md` §12.
- **SAP entries are persisted, not config-driven**: `SAPServiceVerifyProcess` only creates missing rows on a fresh database; it never updates existing rows. To widen `SYSTEM_USER_PASSWORD`/`SYSTEM_DEFAULT` for tests on a reused container, mutate them in-process via `SAPEntryLocalService.updateSAPEntry(...)`. Reference: `SAPTestSetup.java`. Detail in `docs/details/api-liferay-dxp2026.md` §16.
- **configs/${env}/portal-ext.properties shadowing**: The workspace plugin's dockerDeploy task merges `configs/common/` into every env dir, but file-level collisions resolve with env winning. An otherwise-empty env `portal-ext.properties` silently replaces the common file. Keep per-env `portal-ext.properties` absent (or add real overrides) so `configs/common/portal-ext.properties` survives.
- **Docker lifecycle**: The previous Testcontainers approach is replaced by the workspace plugin's native Docker tasks. This eliminates the Testcontainers dependency entirely.
- **License**: DXP requires a valid activation key. Local and CI environments inject it via environment variables.
- **BOM**: Individual API dependencies are replaced by the `release.dxp.api` BOM, which includes journal, DDM, message-boards, and other APIs at the correct version.

## Decision

All eight decisions below were finalized via user Q&A recorded in `.claude/plan/dxp_migration.md`, section 1.

### D1 — Base branch: master, one-shot

Start part 2 from `master`. The first attempt (`feature/migrate_to_dxp` branch) is closed without merging. A single clean PR will carry all DXP 2026 changes. Rationale: avoids carrying forward uncommitted state or partial decisions from the aborted first attempt.

### D2 — Admin password reset partially suppressed via portal-ext.properties; Playwright handles residual `update_password` form

`company.security.update.password.required=false` and `passwords.default.policy.change.required=false` are set in `configs/common/portal-ext.properties`. These eliminate the legacy `PASSWORDRESET` 7-step HTTP ticket flow that was required in the Testcontainers-based harness, so the ticket-handling code is removed from `BaseLiferaySpec`.

**However, DXP 2026 still redirects the default admin to `/c/portal/update_password` on first browser login** even with both properties set. The redirect is a UI-layer interstitial rather than the legacy ticket flow, but it blocks every Playwright spec that tries to navigate after `loginAsAdmin`. `BaseLiferaySpec.loginAsAdmin` therefore submits the form directly — fills `#password1` / `#password2` with a constant `NEW_ADMIN_PASSWORD = 'Test12345'` and submits — when it detects `/c/portal/update_password` in the post-login URL. The login helper also tries `DEFAULT_ADMIN_PASSWORD` first and falls back to `NEW_ADMIN_PASSWORD` so subsequent specs work after the form has been submitted once.

A side effect: every BasicAuth call from tests must use the post-update password, not the original. `BaseLiferaySpec.basicAuthHeader()` is hard-coded to `NEW_ADMIN_PASSWORD`. If the wrong password is sent, `BasicAuthHeaderAutoLoginSupport._getBasicUserId` silently catches `AuthException` and returns the Guest user — there is no surfaced 401 unless `forceBasicAuth=B"true"` is set in the BasicAuth verifier config. Detail: `docs/details/dxp-2026-gotchas.md` §9 (form handling) and §10 (silent Guest fallback).

### D3 — License injection via environment variable

A Gradle task (`resolveLicenseFile`) reads either `LIFERAY_DXP_LICENSE_FILE` (a file path, for local development) or `LIFERAY_DXP_LICENSE_BASE64` (a base64-encoded XML blob, for CI). It writes the result to `configs/local/deploy/activation-key.xml` before `dockerDeploy` copies configs into the Docker image. If both variables are absent the task fails fast with a clear error message. The activation key file is `.gitignore`d to prevent accidental commits.

### D4 — Container lifecycle: autoRemove=false, stop-only on test completion

The Docker container is not removed after the test run. Workspace plugin 16.0.5 sets `autoRemove=true` by default at `RootProjectConfigurator.java:540` (`/home/yasuflatland/tmp/liferay-portal/modules/sdk/gradle-plugins-workspace/src/main/java/com/liferay/gradle/plugins/workspace/configurator/RootProjectConfigurator.java`), so D4 is implemented via an explicit `hostConfig.autoRemove.set(false)` override in the `:createDockerContainer` configure block of `integration-test/build.gradle`. `integrationTest` uses `finalizedBy ':stopDockerContainer'` so the container is stopped but its volume is preserved. This allows post-mortem inspection (logs, `docker exec`, JaCoCo exec file recovery) after test failures. To force volume recreation, run `./gradlew removeDockerContainer` explicitly.

### D5 — Fixed Docker ports: 8080 / 11311 / 8000

The workspace plugin binds fixed ports: `8080:8080` (HTTP), `11311:11311` (GoGo Shell), `8000:8000` (JPDA). There are no dynamically mapped ports. Developers must ensure no other Liferay process is listening on these ports before running `./gradlew startDockerContainer`. The simplicity of fixed ports outweighs the minor inconvenience of stopping a local Liferay when needed.

### D6 — JaCoCo coverage preserved via LIFERAY_JVM_OPTS

The JaCoCo agent is injected into the container JVM through the `LIFERAY_JVM_OPTS` environment variable, set in the `createDockerContainer` task via `withEnvVar(...)`. The agent runs in `tcpserver` mode on port 6300. `BaseLiferaySpec.cleanupSpec()` dumps per-spec `.exec` files; `jacocoIntegrationReport` merges them. Coverage collection is unaffected by the Testcontainers removal.

### D7 — Bundle deployment via dockerDeploy (image-baked)

The module JAR is deployed by `dockerDeploy`, which copies `configs/` overlays (including the JAR from a deploy/ directory) into the Docker build context before the image is built. The JAR is baked into the image layer. Docker's layer cache means subsequent runs with an unchanged JAR reuse the cached layer, making rebuilds fast in practice. The alternative (deploying the JAR into the running container via `docker cp` after startup) is reserved for mid-session development loops and is handled by `LiferayContainer.deployJar(...)`.

### D8 — Workspace plugin pinned to 16.0.5

`com.liferay:com.liferay.gradle.plugins.workspace:16.0.5` is the version certified for DXP 2026.Q1.3-LTS. Workspace plugin 16.x requires all subprojects to be declared explicitly in `settings.gradle` via `include 'modules:liferay-dummy-factory'`. The plugin version is pinned in `settings.gradle` to prevent unexpected behavior from future updates.

## Consequences

### Positive

- **CI is simpler**: No Testcontainers library, no Docker-in-Docker workaround, no dynamic port mapping. The workspace plugin's native Docker tasks handle container lifecycle.
- **JaCoCo preserved**: Coverage from the Liferay container JVM is collected exactly as before; only the injection mechanism changes from Testcontainers `withEnv` to `createDockerContainer.withEnvVar`.
- **Testcontainers removed entirely**: `org.testcontainers:testcontainers` and `org.testcontainers:testcontainers-spock` dependencies are deleted from `integration-test/build.gradle`. `LiferayContainer` becomes a thin POJO.
- **Admin bootstrap is one-shot**: `portal-ext.properties` suppresses the password change requirement at image build time. `BaseLiferaySpec` no longer needs to handle `PASSWORDRESET` flow.
- **`release.dxp.api` BOM**: A single dependency declaration pulls in all DXP 2026 APIs at the correct version. No more per-API version management or skew risk.

### Negative

- **Fixed ports conflict warning**: If another Liferay process is already using port 8080 or 11311, `startDockerContainer` will fail. Developers must stop conflicting processes manually. See `docs/details/dxp-2026-gotchas.md`.
- **License file required**: Without `LIFERAY_DXP_LICENSE_FILE` or `LIFERAY_DXP_LICENSE_BASE64`, the build fails before the container starts. CI must have the `LIFERAY_DXP_LICENSE_BASE64` secret configured.
- **Slower first image build**: The first `startDockerContainer` invocation builds the full Docker image from scratch (~5–10 minutes depending on network). Subsequent runs with an unchanged JAR are fast due to layer caching.

### Neutral

- The JSP taglib URI (`http://xmlns.jcp.org/portlet_3_0`) is unchanged — intentionally. This is not a regression; it reflects the actual capability advertised by DXP 2026. See `adr-0002-portlet-api-javax-namespace.md` for the background on the JSP taglib URI decision.
- `BaseLiferaySpec` retains `ensureBundleActive()` and the GoGo Shell polling mechanism unchanged.
- Playwright version (1.59.0) is unchanged.

## References

- First migration attempt on branch `feature/migrate_to_dxp` (closed — reference only)
- Migration plan: `.claude/plan/dxp_migration.md`
- Expert input: Brian Chan (Portal Architect), Marco Leo (Deployment), David H Nebinger (Test Harness)
- Workspace plugin 16.0.5 source: `/home/yasuflatland/tmp/liferay-portal/modules/sdk/gradle-plugins-workspace`
- Supersedes: `docs/ADR/adr-0001-integration-test-architecture.md` (container strategy section)
- API constraints: `docs/details/api-liferay-dxp2026.md`
- Runtime gotchas: `docs/details/dxp-2026-gotchas.md`
