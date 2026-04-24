# liferay-dummy-factory

Liferay DXP 2026.Q1.3-LTS Workspace: MVCPortlet + React portlet + Spock integration tests against `liferay/dxp:2026.q1.3-lts`.

## Routing — read the matching L2 file for the task you're starting

- New feature, refactor, or general code change → `.claude/rules/writing-code.md`
- Writing or running tests → `.claude/rules/testing.md`
- Bug investigation or test failure analysis → `.claude/rules/debugging.md`
- PR review or code-quality check → `.claude/rules/code-review.md`

## Where to find concrete details and history

- Concrete commands, selectors, version pins, API constraints → `docs/details/` (read on demand)
- Past architectural decisions and their rationale → `docs/ADR/` (read on demand)
- Workflow API specifics live in `docs/details/workflow-api.md`; it is the source of truth for `site.create`, `organization.create`, and the taxonomy-only startup fallback.
- `.claude/plan/` is gitignored — it is a scratch location for orchestration plans that never lands in PRs. Do not expect plan files to be part of a diff.

## Core contracts — break these and things go silently wrong

1. **Input boundary policy** — reject user input at the boundary; sanitize external-generated data. Never mix the two strategies.
2. **Single source of truth** — every fact, rule, or contract lives in exactly one file. Other files link to it.
3. **Creator pattern** — batch `*Creator` classes wrap per-entity work in `TransactionInvokerUtil.invoke` + `throws Throwable`. Detail in `.claude/rules/writing-code.md`.
4. **Batch response contract** — Creators return `{success, count, requested, skipped, error?, items}` with `success := created == requested` (strict). `error` MUST be set whenever `success == false`.
5. **JSONWS-first verification** — test post-conditions through `/api/jsonws/...`, not Playwright UI navigation. Detail in `.claude/rules/testing.md`.
6. **`jakarta.portlet` 4.0** — DXP 2026.Q1.3-LTS uses `jakarta.portlet.*` imports and `jakarta.portlet.version=4.0` component properties. JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0` (JCP namespace). See `docs/ADR/adr-0008-dxp-2026-migration.md`.
7. **`data-testid` is mechanically named** — `${entityKey}-${kebab(field)}-${typeSuffix}`. Do not invent ids; follow the contract in `.claude/rules/writing-code.md`.
8. **One package manager per repo** — `yarn.lock` only. Never coexist with `package-lock.json`.

## Common Gradle commands

```bash
./gradlew :modules:liferay-dummy-factory:jar           # Build the bundle JAR
./gradlew :modules:liferay-dummy-factory:test          # Host-JVM unit tests + JaCoCo
./gradlew :integration-test:integrationTest             # Spock + Workspace-native Docker (DXP 2026)
./gradlew startDockerContainer                          # Start DXP 2026 container (for local dev loops)
./gradlew stopDockerContainer                           # Stop the container (keeps volume; reuse next run)
./gradlew removeDockerContainer                         # Hard clean (force volume recreation)
```
