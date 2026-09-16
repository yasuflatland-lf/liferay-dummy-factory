# Dependency version pinning — JS / React toolchain

L3 detail. Source of truth for the version-pinning rules of the JavaScript and React toolchain. Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/testing.md`.

## Exact-pin the vite / vitest / plugin-react / jsdom chain

Caret (`^`) ranges on `vite`, `vitest`, `@vitejs/plugin-react`, and `jsdom` silently pull in newer minors whose Node engine requirements have moved. `vite ^6.2.0` will resolve to `6.3.x`, and vite `6.3+` requires Node `^20.19 || >=22.12`, which breaks on Node 20.12.

Write every one of these packages as an **exact version** (no `^`, no `~`); it is the only stable answer short of upgrading Node.

The same rule applies to any tool downstream of the Node engine matrix. If a transitive package starts asserting a higher minimum Node version, the only fix that does not destabilise the rest of the toolchain is exact-pinning the immediate dependency.

## Current pinned matrix (Node 20.12.2, engines ignored)

| Package                | Version  |
| ---------------------- | -------- |
| `vitest`               | `4.1.11` |
| `@vitest/coverage-v8`  | `4.1.11` |
| `vite`                 | `7.3.5`  |
| `@vitejs/plugin-react` | `5.2.0`  |
| `jsdom`                | `25.0.1` |
| `esbuild`              | `0.28.1` |

`vite 7`, `@vitejs/plugin-react 5` and `vitest 4` all declare `engines.node >= 20.19`, but the workspace still runs Node 20.12.2. This works because the Gradle `yarnInstall` task (the only install path CI uses) runs `yarn install --frozen-lockfile --ignore-engines`. A bare `yarn install` on a developer shell therefore fails with `The engine "node" is incompatible with this module` — run `yarn install --ignore-engines` locally, or go through `./gradlew yarnInstall`. `docs/ADR/adr-0005-node-vitest-version-pinning.md` records the original Node 20.12 matrix (vitest 2.1.8 / vite 6.2.7 / plugin-react 4.3.4); that constraint was superseded once `--ignore-engines` became the install path, but the exact-pin rule it introduced still stands.

Node engine mismatches surface only during the `yarn install` fetch phase, not at resolve time, so the failure looks like a transient network error — treat any post-bump install failure as a pinning regression first.

## `@vitest/coverage-v8` lockstep with `vitest`

`@vitest/coverage-v8` declares an exact peer dependency on the same `vitest` version (`4.1.11` ↔ `4.1.11`). Bump both in the same commit; Yarn 1 only warns on peer mismatch, so a lone `vitest` bump passes `yarn install` and then fails at `npx vitest run --coverage` in `unit-test.yml`.

## `resolutions.vite` in the root `package.json`

`vitest 4` lists `vite` as a regular dependency (`^6.0.0 || ^7.0.0 || ^8.0.0`), not only as a peer. On a fresh resolution Yarn 1 picks the newest match (`vite 8.x`) and nests it under `node_modules/vitest/node_modules/vite`, which drags in a third `esbuild` line (`^0.28.2`) next to the top-level `0.28.1` pin used by `scripts/build.mjs` and the `^0.27.0` copy nested under `vite 7`. The resulting hoisting layout makes the nested copies find the wrong `@esbuild/<platform>` binary, and `yarn install` dies in esbuild's post-install check (`install.js`: `Expected "0.27.7" but got "0.28.1"`). The root `package.json` therefore carries:

```json
"resolutions": {
	"vite": "7.3.5"
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
