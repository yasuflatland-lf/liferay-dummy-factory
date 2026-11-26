---
name: liferay-debug
description: Investigate a failing build, bundle, integration test, JSONWS/Basic Auth error or Playwright spec in this Liferay workspace. Use when a test fails, the bundle is not Active, the portlet does not render, or a JSONWS call returns 401/403/404 or Guest data.
---

# Debug a Liferay Dummy Factory failure

1. **Reproduce and read the real error.** Run the narrowest command (`./gradlew :integration-test:integrationTest --tests "<spec>" --info`, or the unit task) and read the first failure, not the last.
2. **Rule out stale state before theorizing.** A pass or failure that makes no sense is often a kept container volume or a replayed Gradle result. `./gradlew removeDockerContainer`, and for frontend-toolchain changes `./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean`, then rerun.
3. **Look up the symptom** in `docs/guides/troubleshooting.md` (bundle, container, JSONWS/auth, Playwright tables). Follow its link to the reference section for the mechanism.
4. **Inspect the live system** with the commands at the top of the troubleshooting guide: container log, GoGo shell (`lb`, `diag`, `headers`, `scr:info`), JSONWS via `curl`.
5. **Fix the root cause.** No fallbacks that hide the failure, no skipped or weakened assertions, no `setForce(true)`, no `--no-verify`.
6. **Record new knowledge once.** A new Liferay behaviour goes into `docs/reference/liferay-dxp-api.md` or `docs/reference/dxp-runtime-config.md`, plus one row in the troubleshooting table that links to it. Run `node scripts/check-docs.mjs`.
