# liferay-dummy-factory

Liferay DXP 2026.Q1.3-LTS Workspace: MVCPortlet + React portlet + Spock integration tests against `liferay/dxp:2026.q1.3-lts`.

## Routing — read the matching L2 file for the task you're starting

- Feature / refactor / general code change → `.claude/rules/writing-code.md` · Tests → `.claude/rules/testing.md`
- Bug or test-failure investigation → `.claude/rules/debugging.md` · PR / code-quality review → `.claude/rules/code-review.md`

## Where to find concrete details and history

- `docs/details/` — concrete commands, selectors, version pins, API constraints (read on demand); `workflow-api.md` is SoT for `site.create` / `organization.create` / taxonomy-only startup fallback. `docs/ADR/` — past decisions.
- `.claude/plan/` — gitignored scratch for orchestration plans; never lands in a diff.

## Core contracts — break these and things go silently wrong

1. **Input boundary policy** — reject user input at the boundary; sanitize external-generated data. Never mix the two strategies.
2. **Single source of truth** — every fact, rule, or contract lives in exactly one file. Other files link to it.
3. **Creator + batch response contract** — `*Creator` classes wrap per-entity work in `TransactionInvokerUtil.invoke` + `throws Throwable` and return `{success, count, requested, skipped, error?, items}` with strict `success := created == requested`; `error` MUST be set whenever `success == false`. Detail in `.claude/rules/writing-code.md`.
4. **JSONWS-first verification** — test post-conditions through `/api/jsonws/...`, not Playwright UI navigation. Detail in `.claude/rules/testing.md`.
5. **`jakarta.portlet` 4.0** — DXP 2026.Q1.3-LTS uses `jakarta.portlet.*` imports and `jakarta.portlet.version=4.0` component properties. JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0` (JCP namespace). See `docs/ADR/adr-0008-dxp-2026-migration.md`.
6. **`data-testid` is mechanically named** — `${entityKey}-${kebab(field)}-${typeSuffix}`. Do not invent ids; follow the contract in `.claude/rules/writing-code.md`.
7. **One package manager per repo** — `yarn.lock` only. Never coexist with `package-lock.json`.

## Common Gradle commands

```bash
./gradlew :modules:liferay-dummy-factory:jar              # Build the bundle JAR
./gradlew :modules:liferay-dummy-factory:test             # Host-JVM unit tests + JaCoCo
./gradlew :integration-test:integrationTest               # Spock + Workspace-native Docker (DXP 2026)
./gradlew startDockerContainer                            # Start DXP 2026 container (for local dev loops)
./gradlew stopDockerContainer                             # Stop the container (keeps volume; reuse next run)
./gradlew removeDockerContainer                           # Hard clean (force volume recreation)
./gradlew :modules:liferay-dummy-factory:copyJarToLatest  # Release only — publish JAR to latest/; see docs/details/testing-gradle.md
```
