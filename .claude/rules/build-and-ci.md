---
paths:
  - "**/build.gradle"
  - "settings.gradle"
  - "gradle.properties"
  - "gradle/**"
  - "**/package.json"
  - "yarn.lock"
  - "mise.toml"
  - "renovate.json"
  - ".github/**"
  - "configs/**"
---

# Build, dependency and CI rules

Facts and constraints: `docs/reference/dependencies.md`, `docs/reference/test-harness.md`, `docs/reference/dxp-runtime-config.md`.

- **Versions are pinned in exactly one file each** (table in `docs/reference/dependencies.md`). Never copy a version into documentation; `scripts/check-docs.mjs` rejects it.
- **Yarn only.** Never add `package-lock.json`; never hand-edit `yarn.lock` (take one side, `yarn install`, rerun tests).
- **Lockstep pairs** (listed in `docs/reference/dependencies.md`) move in the same commit.
- **Liferay target** changes `liferay.workspace.product` and `liferay.workspace.target.platform.version` together, then follows "Upgrading the Liferay target".
- **Renovate guard-rails** in `renovate.json` carry their reasons; keep a reason on every new rule.
- **`copyJarToLatest`** stays a manual release task; never hook it to `jar`.
- **Integration-test Gradle wiring**: Docker `hostConfig.binds` set at configuration time; port 6300 needs `exposePorts` plus a binding without host-IP prefix; `integrationTest` keeps `jacoco { enabled = false }` and `evaluationDependsOn(':modules:liferay-dummy-factory')` stays at the top.
- **`configs/`**: `.config` files use typed literals (`B"true"`), at most one comment line, comma-separated strings for `urlsIncludes`; `configs/common/portal-liferay-online-config.properties` stays empty; no empty per-env `portal-ext.properties`.
- **GitHub Actions**: when editing a workflow, check each action pin is on its latest major (Renovate also proposes bumps). CI must keep running `node scripts/check-docs.mjs`.
- After any frontend-toolchain change, prove it with a clean integration run (`:modules:liferay-dummy-factory:clean :integration-test:clean`, then `integrationTest`).
