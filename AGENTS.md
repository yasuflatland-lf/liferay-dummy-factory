# AGENTS.md

Instructions for AI coding agents working in this repository — Codex, Claude Code, and any tool that reads `AGENTS.md`. Humans start at [`README.md`](README.md) and [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Project

`liferay-dummy-factory` is a Liferay DXP Control Panel app that generates dummy data (users, sites, pages, web content, documents, taxonomy, message boards, …) through per-entity forms and multi-step JSON workflows. One OSGi bundle (MVCPortlet + JAX-RS workflow API + React UI) in a Liferay Workspace, tested with JUnit, Vitest, and Spock + Playwright against a real DXP container. The exact DXP version is in `gradle.properties`.

| Path | What |
|---|---|
| `modules/liferay-dummy-factory/src/main/java/` | `…/tools/portlet/actions` (resource commands) → `…/tools/service` (Creators); `…/tools/workflow/` (engine, adapters, JAX-RS) |
| `modules/liferay-dummy-factory/src/main/resources/` | `META-INF/resources/` (`view.jsp`, React/TypeScript in `js/`), `content/Language.properties` |
| `modules/liferay-dummy-factory/src/test/`, `modules/liferay-dummy-factory/test/` | JUnit, Vitest |
| `integration-test/` | Spock + Playwright specs, workflow sample fixtures |
| `configs/common/` | portal and OSGi config baked into the test image |
| `docs/` | all explanations and facts — map in [`docs/README.md`](docs/README.md) |

## Commands

```bash
./gradlew :modules:liferay-dummy-factory:jar                     # build the bundle (includes the esbuild frontend build)
./gradlew :modules:liferay-dummy-factory:test                    # Java unit tests + JaCoCo
(cd modules/liferay-dummy-factory && yarn test && yarn checkFormat)  # Vitest + formatting
./gradlew :integration-test:compileTestGroovy                    # compile specs without Docker
./gradlew :integration-test:integrationTest [--tests "<FQCN>"]   # full E2E; needs Docker + LIFERAY_DXP_LICENSE_FILE
./gradlew startDockerContainer | stopDockerContainer | removeDockerContainer
node scripts/check-docs.mjs                                      # docs + agent-instruction consistency (CI-enforced)
```

## Contracts that fail silently when broken

1. **Input boundary** — reject user input, sanitize generated data, never mix. → [backend](docs/architecture/backend.md#input-boundary-policy)
2. **Batch contract** — Creators return `BatchResult<T>` → `{success, count, requested, skipped, items, error?}`, `success := count == requested`, `error` iff failure; per-entity `BatchTransaction.run`, `throws Throwable`. → [backend](docs/architecture/backend.md#batch-response-contract)
3. **Two entry points, one behaviour** — resource command and workflow adapter call the same Creator and expose identical item fields; new `/ldf/*` commands are registered in `view.jsp`. → [workflow API](docs/reference/workflow-api.md#rc--workflow-adapter-field-parity)
4. **DXP 2026 platform** — `jakarta.portlet` 4.0 (JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0`), no `javax.servlet` import, `release.dxp.api` without a version. → [DXP API](docs/reference/liferay-dxp-api.md#dependencies-and-packaging)
5. **Tests verify through JSONWS**; Playwright only for DOM concerns, never selecting by visible text, always asserting the success class. → [`integration-test/AGENTS.md`](integration-test/AGENTS.md)
6. **`data-testid`s are generated**, never invented. → [test ids](docs/reference/test-ids.md)
7. **One fact, one file; no versions in prose; Yarn only.** → [`docs/AGENTS.md`](docs/AGENTS.md), [dependencies](docs/reference/dependencies.md)

## Scoped instructions

More rules live next to the code they govern. **Before editing a file, read every `AGENTS.md` on the path from the repository root to that file**; the deeper file wins on conflict. Some tools load these automatically, others only for the directory they were started in — so read them yourself.

| File | Governs |
|---|---|
| [`modules/liferay-dummy-factory/AGENTS.md`](modules/liferay-dummy-factory/AGENTS.md) | the bundle: `bnd.bnd`, `build.gradle`, frontend toolchain |
| [`modules/liferay-dummy-factory/src/main/java/AGENTS.md`](modules/liferay-dummy-factory/src/main/java/AGENTS.md) | Creators, resource commands, workflow adapters, Java style |
| [`modules/liferay-dummy-factory/src/main/resources/AGENTS.md`](modules/liferay-dummy-factory/src/main/resources/AGENTS.md) | React/TypeScript, JSP, i18n |
| [`modules/liferay-dummy-factory/src/test/AGENTS.md`](modules/liferay-dummy-factory/src/test/AGENTS.md) | JUnit tests |
| [`modules/liferay-dummy-factory/test/AGENTS.md`](modules/liferay-dummy-factory/test/AGENTS.md) | Vitest tests |
| [`integration-test/AGENTS.md`](integration-test/AGENTS.md) | test design for all suites; Spock, Playwright, harness wiring |
| [`configs/AGENTS.md`](configs/AGENTS.md) | portal and OSGi configuration |
| [`docs/AGENTS.md`](docs/AGENTS.md) | every Markdown file: placement, single source, links |
| [`.github/AGENTS.md`](.github/AGENTS.md) | GitHub Actions workflows |

## Skills

Procedures in the open [Agent Skills](https://agentskills.io) format under `.agents/skills/`. Use one when its trigger matches.

| Skill | Use when |
|---|---|
| [`liferay-debug`](.agents/skills/liferay-debug/SKILL.md) | any failing build, bundle, JSONWS call or test |
| [`contract-review`](.agents/skills/contract-review/SKILL.md) | reviewing a diff or PR, or before declaring a change done |
| [`parallel-orchestration`](.agents/skills/parallel-orchestration/SKILL.md) | only when the user explicitly asks for multi-agent work |

## Root build and dependency files

Applies to `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `package.json`, `yarn.lock`, `mise.toml`, `renovate.json`. Facts: [dependencies](docs/reference/dependencies.md).

- Each version is pinned in exactly one file. Never copy a version into documentation.
- Yarn only. Never add `package-lock.json`; never hand-edit `yarn.lock` (take one side, `yarn install`, rerun tests). Lockstep pairs move in the same commit.
- A Liferay target change moves `liferay.workspace.product` and `liferay.workspace.target.platform.version` together, then follows [Upgrading the Liferay target](docs/reference/dependencies.md#upgrading-the-liferay-target).
- Every Renovate rule in `renovate.json` carries its reason.
- `copyJarToLatest` stays a manual release task ([release guide](docs/guides/release.md)); never hook it to `jar`, and ordinary changes do not touch `latest/`.

## Working agreements

- Look in `docs/` before re-deriving anything; record a new finding there, once ([placement rules](docs/AGENTS.md)).
- Fix root causes. No fallbacks that hide a failure, no skipped or weakened assertions, no `--no-verify`.
- Commits and PRs follow [`CONTRIBUTING.md`](CONTRIBUTING.md#commits-and-pull-requests); significant design decisions get an [ADR](docs/adr/README.md).
- Scratch notes go in `.agents/plan/` (git-ignored). Personal instructions go in `AGENTS.override.md` or `CLAUDE.local.md` (both git-ignored).
- These instructions are tool-neutral by design: edit `AGENTS.md` files and `.agents/skills/`, never tool-specific copies. How the files fit together: [agent instructions](docs/reference/agent-instructions.md).

## Done means

Code, tests and docs changed together; the relevant unit suites pass; `node scripts/check-docs.mjs` passes; integration specs at least compile (`compileTestGroovy`). If you could not run the integration suite (it needs Docker and a DXP license), say so explicitly.
