# ADR-0010: Backport to Liferay DXP 2025.Q1.11 LTS

## Status: Accepted (2026-10-06)

## Context

`master` targets Liferay DXP 2026.Q3.6 (Jakarta EE). A maintenance branch is needed for DXP 2025.Q1.11 LTS, which uses Java EE 8 (`javax`). Liferay switched from Java EE 8 to Jakarta EE in 2025.Q3.

Cut `2025.Q1.11` from `master` to retain the latest fixes, including #97 (BOM follows the target platform) and #109/#116 (`ensureBundleActive` waits for redeployment). The 2026.Q1.x LTS branches also use Jakarta EE, so they provide no namespace advantage. The legacy `2024.q3.9` 1.x codebase was rejected because it would lose the current fixes and architecture.

## Decision

- **Target platform** — Set `liferay.workspace.product=dxp-2025.q1.11-lts` and `liferay.workspace.target.platform.version=2025.q1.11`. Use `liferay/dxp:2025.q1.11-lts`. Keep `release.dxp.api` without a `version:` so the workspace target platform selects the BOM version (see `docs/details/api-liferay-dxp2026.md` §1).
- **Java EE namespace** — Rename `jakarta.portlet`, `jakarta.servlet`, and `jakarta.ws.rs` imports to `javax.portlet`, `javax.servlet`, and `javax.ws.rs`. Use Portlet API 3.0: `javax.portlet.version=3.0`. Rename all other portlet component property keys to `javax.portlet.*`, including `name`, `init-param.*`, `display-name`, and `resource-bundle`; rename the language title key to `javax.portlet.title.<portletName>`. Keep the JSP taglib URI `http://xmlns.jcp.org/portlet_3_0` unchanged.
- **OSGi imports** — Change `Import-Package: !javax.servlet,!javax.servlet.http,*` to `Import-Package: *`. DXP 2025.Q1 exports both servlet packages, and the bundle needs them to load `HttpServletRequest`. The built JAR imports `javax.portlet`, `javax.servlet`, `javax.servlet.http`, `javax.ws.rs`, and `javax.ws.rs.core`, with no `jakarta` imports.
- **Service signatures** — Use the 15-argument `GroupLocalService.addGroup` overload, without `externalReferenceCode` or `typeSettings`, verified with `javap` on `release.dxp.api-2025.q1.11.jar`. Use the 9-argument `AssetCategoryLocalService.addCategory` overload, without the `boolean system` added in 2026.Q3. Exact signatures live in `docs/details/api-liferay-dxp2026.md` §2 and §24. Other documented signatures compile unchanged, including `CompanyLocalService.addCompany` (13 arguments), `MBCategoryLocalService.addCategory`, and `UserLocalService.addUserWithWorkflow` with `int type`.
- **Maintenance flow** — Fixes flow from `master` to this branch via cherry-pick, with the `javax` rename re-applied where needed. Keep the backport deltas limited to platform compatibility.

## Consequences

- The branch retains the current implementation and test harness while targeting the Java EE 8 LTS runtime.
- Compilation succeeds against `2025.q1.11`; all 244 unit tests pass on the host JVM.
- Cherry-picked changes require review for namespace and service-signature compatibility.
- Existing detail filenames remain stable; their introductions identify the branch target. ADR-0002 and ADR-0008 remain historical records.

Runtime verification (2026-10-06): `:integration-test:integrationTest --no-daemon` against `liferay/dxp:2025.q1.11-lts` passed 99/99 (0 failed, 0 skipped). The existing test harness (BasicAuth/SAP test setup components, `configs/common` overlays, `/api/jsonws/` base path) works unchanged on this runtime.
