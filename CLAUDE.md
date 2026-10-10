# liferay-dummy-factory

A Liferay DXP Control Panel app that generates dummy data (users, sites, pages, web content, documents, taxonomy, message boards, …) through per-entity forms and multi-step JSON workflows. One OSGi bundle (MVCPortlet + JAX-RS workflow API + React UI) in a Liferay Workspace, tested with JUnit, Vitest, and Spock + Playwright against a real DXP container. The exact DXP version is in `gradle.properties`.

## Map

| Path | What |
|---|---|
| `modules/liferay-dummy-factory/src/main/java/…/tools/` | `portlet/actions` (resource commands) → `service` (Creators) ; `workflow/` (engine, adapters, JAX-RS) |
| `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/` | `view.jsp`, React/TypeScript in `js/` |
| `modules/liferay-dummy-factory/src/test/java/`, `…/test/` | JUnit, Vitest |
| `integration-test/` | Spock + Playwright specs, workflow sample fixtures |
| `configs/common/` | portal and OSGi config baked into the test image |
| `docs/` | documentation — start at `docs/README.md` |

## Commands

```bash
./gradlew :modules:liferay-dummy-factory:jar                 # build the bundle (includes the esbuild frontend build)
./gradlew :modules:liferay-dummy-factory:test                # Java unit tests + JaCoCo
(cd modules/liferay-dummy-factory && yarn test)              # Vitest
./gradlew :integration-test:compileTestGroovy                # compile specs without Docker
./gradlew :integration-test:integrationTest [--tests "<FQCN>"]   # full E2E; needs Docker + LIFERAY_DXP_LICENSE_FILE
./gradlew startDockerContainer | stopDockerContainer | removeDockerContainer
node scripts/check-docs.mjs                                  # docs consistency (CI-enforced)
```

## Contracts that fail silently when broken

1. **Input boundary** — reject user input, sanitize generated data, never mix. → `docs/architecture/backend.md#input-boundary-policy`
2. **Batch contract** — Creators return `BatchResult<T>` → `{success, count, requested, skipped, items, error?}`, `success := count == requested`, `error` iff failure; per-entity `BatchTransaction.run`, `throws Throwable`. → `docs/architecture/backend.md#batch-response-contract`
3. **Two entry points, one behaviour** — resource command and workflow adapter call the same Creator and expose identical item fields; new `/ldf/*` commands are registered in `view.jsp`. → `docs/reference/workflow-api.md`
4. **DXP 2026 platform** — `jakarta.portlet` 4.0 (JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0`), no `javax.servlet` import, `release.dxp.api` without a version. → `docs/reference/liferay-dxp-api.md`
5. **Tests verify through JSONWS**; Playwright only for DOM concerns, never selecting by visible text, always asserting the success class. → `.claude/rules/tests.md`
6. **`data-testid`s are generated**, never invented. → `docs/reference/test-ids.md`
7. **One fact, one file; no versions in prose; Yarn only.** → `.claude/rules/documentation.md`, `docs/reference/dependencies.md`

## How guidance is organized

- `.claude/rules/*.md` are **path-scoped** and load automatically when you touch matching files: `backend-java`, `frontend`, `tests`, `build-and-ci`, `documentation`.
- `.claude/skills/` hold task procedures: `liferay-debug` (any failure), `contract-review` (reviews), `parallel-orchestration` (only on explicit request).
- Explanations and facts live in `docs/` (guides / reference / architecture / adr). Look there before re-deriving anything; record new findings there, once.
- `.claude/plan/` is git-ignored scratch space.

## Done means

Code, tests and docs changed together; the relevant unit suites pass; `node scripts/check-docs.mjs` passes; integration specs at least compile (`compileTestGroovy`). If you could not run the integration suite (it needs Docker and a DXP license), say so explicitly.
