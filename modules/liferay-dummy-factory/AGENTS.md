# Module `liferay-dummy-factory`

Scope: the OSGi bundle — `bnd.bnd`, `build.gradle`, `package.json`, the esbuild/Vitest/TypeScript configs and `scripts/`. Also obey the root [`AGENTS.md`](../../AGENTS.md). Deeper files add rules for `src/main/java/`, `src/main/resources/`, `src/test/` and `test/`.

## Platform (DXP 2026)

Facts and reasons: [`docs/reference/liferay-dxp-api.md`](../../docs/reference/liferay-dxp-api.md#dependencies-and-packaging).

- `bnd.bnd` keeps `Import-Package: !javax.servlet,!javax.servlet.http,*`.
- `release.dxp.api` has **no** version (never `"default"`), and no per-API artifacts beside it.

## Frontend toolchain

Facts: [`docs/reference/dependencies.md`](../../docs/reference/dependencies.md).

- Production build is `modules/liferay-dummy-factory/scripts/build.mjs` (esbuild); do not move it to Vite or `@liferay/npm-scripts` ([ADR-0006](../../docs/adr/0006-custom-esbuild-over-npm-scripts.md)).
- Yarn only; exact versions for the Node-sensitive toolchain; respect the lockstep pairs.
- After a toolchain change, prove it with a **clean** integration run (`:modules:liferay-dummy-factory:clean :integration-test:clean`, then `integrationTest`) — see the [incremental build trap](../../docs/reference/test-harness.md#incremental-build-trap).
