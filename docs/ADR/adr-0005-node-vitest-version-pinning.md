# ADR-0005: Node 20.12 Constraint Forces Exact Version Pins for Vitest Toolchain

## Status

Accepted

## Date

2026-04-12

## Context

The Liferay Workspace project runs on Node 20.12.2 — both locally and in CI — and we were upgrading React from 18.2.0 to 19.2.5 while migrating the unit test suite from Jest 27.5.1 to Vitest. The original request was to use `vite ^6.2.0` (caret semver), which felt like a reasonable, low-ceremony pin.

The first install attempt combined `vite ^6.2.0` with `vitest ^3.0.0` and `@vitejs/plugin-react ^5.0.0`. `yarn install` failed with engine errors demanding Node `^20.19.0 || >=22.12.0`. Digging into the release notes revealed a chained constraint we had not seen coming:

- Vite 6.3+ raised its Node baseline to 20.19+.
- Vitest 3.x transitively requires Vite 7 and therefore Node 20.19+.
- `@vitejs/plugin-react` 5.x also requires Node 20.19+.

Any caret-based range on any of these three packages resolves to the latest satisfying version, and for all three the latest satisfying version now means engine incompatibility on Node 20.12. Caret semver — the default yarn/npm idiom — actively fights us here: every `yarn install` after a new upstream release would silently re-break the workspace. We needed a dependency specification strategy that was stable under a fixed Node version, not just "stable right now."

## Decision

Pin the entire Vitest-adjacent toolchain to exact versions (no caret, no tilde) — specifically the newest releases that are still compatible with Node 20.12:

- `vite`: `6.2.7`
- `vitest`: `2.1.8`
- `@vitejs/plugin-react`: `4.3.4`
- `@vitest/coverage-v8`: `2.1.8` (later dropped as unused)
- `jsdom`: `25.0.1`
- `@types/react`: `19.2.3`
- `@types/react-dom`: `19.2.3` (matched to `@types/react` because the latter does not have a higher `19.2.x` patch available)

These pins live in `modules/liferay-dummy-factory/package.json` as bare version strings, with no caret prefix, so yarn's resolver cannot pick a newer patch that re-introduces the engine mismatch.

## Alternatives Considered

- **Upgrade Node to 22.12+** — would allow normal caret semver and the latest Vitest 3.x / Vite 7 line. Rejected because it requires coordinating a Node bump across CI, developer shells, and any downstream Liferay Workspace conventions. That is a separate change and was explicitly out of scope for the React 19 / Vitest migration PR.
- **Stay on Jest** — rejected because the migration to Vitest was the explicit goal of the work; a Jest toolchain also has its own React 19 compatibility issues.
- **Use caret ranges with yarn `resolutions` overrides** — rejected. `resolutions` is a workspace-level override and is fragile against future peer dependency changes; an exact pin on the direct dependency is simpler, more auditable, and fails loudly when someone attempts a manual bump.
- **Replace `resolve.alias` with a regex for React single-instance deduplication** — rejected after finding that a regex alias breaks `react/jsx-dev-runtime` subpath resolution. Use `resolve.dedupe: ['react', 'react-dom']` in `vitest.config.ts` instead. This is a separate but related finding that travelled with the same PR.

## Consequences

### Positive

- `yarn install` is deterministic and engine-compatible on Node 20.12 — the workspace cannot silently break because a transitive dependency shipped a new minor.
- The chosen combination has been validated against the full test suite (28 Vitest unit tests, 63 integration tests green).
- Future readers who ask "why isn't this `^6.2.0`?" are answered by this ADR instead of re-running the same investigation.

### Negative

- Dependabot / Renovate cannot auto-bump any of these packages without manual review, because any caret-range bot PR would immediately re-introduce the engine mismatch.
- Minor security patches within the `6.2.x` and `2.1.x` lines are not picked up automatically; they must be reviewed by a human.
- The pins are time-bound: they are correct only as long as Node 20.12 is the project's runtime floor.

### Mitigation

When the project eventually bumps Node to the next LTS (22.x+), revisit this ADR, relax the pins to caret, and delete the explanatory note once the constraint no longer applies.

## References

- `modules/liferay-dummy-factory/package.json` — exact pins
- `modules/liferay-dummy-factory/vitest.config.ts` — uses `resolve.dedupe: ['react', 'react-dom']`
- `.claude/rules/writing-code.md` — `## One package manager per repo` section (dependency version pinning reference)
- `.claude/rules/testing.md` — Vitest Migration Gotchas section
- React upgraded to 19.2.5 and tests migrated to Vitest
