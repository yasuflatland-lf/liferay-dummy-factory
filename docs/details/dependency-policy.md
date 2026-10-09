# Dependency version pinning — JS / React toolchain

L3 detail. Source of truth for the version-pinning rules of the JavaScript and React toolchain. Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/testing.md`.

## Exact-pin the vite / vitest / plugin-react / jsdom chain

Caret (`^`) ranges on `vite`, `vitest`, `@vitejs/plugin-react`, and `jsdom` silently pull in newer minors whose Node engine requirements have moved. `vite ^6.2.0` will resolve to `6.3.x`, and vite `6.3+` requires Node `^20.19 || >=22.12`, which breaks on Node 20.12.

Write every one of these packages as an **exact version** (no `^`, no `~`); it is the only stable answer short of upgrading Node.

The same rule applies to any tool downstream of the Node engine matrix. If a transitive package starts asserting a higher minimum Node version, the only fix that does not destabilise the rest of the toolchain is exact-pinning the immediate dependency.

## Current pinned matrix (Node 24.21.0)

| Package                | Version  |
| ---------------------- | -------- |
| `vitest`               | `5.0.3`  |
| `@vitest/coverage-v8`  | `5.0.3`  |
| `vite`                 | `8.3.2`  |
| `@vitejs/plugin-react` | `6.1.1`  |
| `jsdom`                | `30.1.1` |
| `esbuild`              | `0.28.2` |
| `typescript`           | `6.0.3`  |

CI (`unit-test.yml`, `integration-test.yml`) and `mise.toml` run Node 24. The Gradle `yarnInstall` task still runs `yarn install --frozen-lockfile --ignore-engines`, so a future package whose `engines.node` moves past the CI Node version installs cleanly but may fail at runtime. jsdom 30 is such a case: on Node 20 every Vitest worker dies with `webidl.util.markAsUncloneable is not a function`, which only went away once CI moved to Node 24. `docs/ADR/adr-0005-node-vitest-version-pinning.md` records the original Node 20.12 matrix (vitest 2.1.8 / vite 6.2.7 / plugin-react 4.3.4); the exact-pin rule it introduced still stands.

TypeScript 6 rejects the deprecated `moduleResolution: "node"` (node10) and `baseUrl` compiler options, so `tsconfig.json` uses `moduleResolution: "bundler"` and no `baseUrl`.

Node engine mismatches surface only during the `yarn install` fetch phase, not at resolve time, so the failure looks like a transient network error — treat any post-bump install failure as a pinning regression first.

## `@vitest/coverage-v8` lockstep with `vitest`

`@vitest/coverage-v8` declares an exact peer dependency on the same `vitest` version (`5.0.3` ↔ `5.0.3`). Bump both in the same commit; Yarn 1 only warns on peer mismatch, so a lone `vitest` bump passes `yarn install` and then fails at `npx vitest run --coverage` in `unit-test.yml`.

## `resolutions.vite` in the root `package.json`

Historical context: `vitest 4` listed `vite` as a regular dependency (`^6.0.0 || ^7.0.0 || ^8.0.0`), not only as a peer. On a fresh resolution Yarn 1 picks the newest match (`vite 8.x`) and nests it under `node_modules/vitest/node_modules/vite`, which drags in a third `esbuild` line (`^0.28.2`) next to the top-level `0.28.1` pin used by `scripts/build.mjs` and the `^0.27.0` copy nested under `vite 7`. The resulting hoisting layout made the nested copies find the wrong `@esbuild/<platform>` binary, and `yarn install` died in esbuild's post-install check (`install.js`: `Expected "0.27.7" but got "0.28.1"`). `vitest 5` no longer depends on `vite` and `vite 8` no longer depends on `esbuild`, but the root `package.json` still carries the pin so a stray transitive `vite` request cannot nest a second copy:

```json
"resolutions": {
	"vite": "8.3.2"
}
```

which collapses every `vite` request onto the single pinned copy. Keep this value equal to the module's `vite` pin whenever `vite` is bumped; a Dependabot PR that bumps only one of the two will reintroduce the nested copy.

## `@types/react` / `@types/react-dom` lockstep

`@types/react-dom` patch releases lag `@types/react`, and an exact matching patch is often not published. Always align both packages to the highest patch version available on DefinitelyTyped for which **both** exist; do not bump one without the other.

A version skew does not fail the build but silently corrupts types such as `ReactNode`, producing downstream type errors in unrelated files.

## Cross-references

- The Vitest migration gotchas (Mock typing, RTL cleanup, vi.mock hoisting, React double-resolution, ESM setup) live in `docs/details/ui-vitest-gotchas.md`.
- The Playwright Java vs Node version skew rule lives in `docs/details/testing-playwright.md`.
- The single-package-manager rule (no coexistence of `package-lock.json` and `yarn.lock`) lives in `.claude/rules/writing-code.md`.
