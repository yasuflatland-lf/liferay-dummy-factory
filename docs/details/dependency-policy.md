# Dependency version pinning — JS / React toolchain

L3 detail. Source of truth for the version-pinning rules of the JavaScript and React toolchain. Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/testing.md`.

## Exact-pin the vite / vitest / plugin-react / jsdom chain

Caret (`^`) ranges on `vite`, `vitest`, `@vitejs/plugin-react`, and `jsdom` silently pull in newer minors whose Node engine requirements have moved. `vite ^6.2.0` will resolve to `6.3.x`, and vite `6.3+` requires Node `^20.19 || >=22.12`, which breaks on Node 20.12.

Write every one of these packages as an **exact version** (no `^`, no `~`); it is the only stable answer short of upgrading Node.

The same rule applies to any tool downstream of the Node engine matrix. If a transitive package starts asserting a higher minimum Node version, the only fix that does not destabilise the rest of the toolchain is exact-pinning the immediate dependency.

## Node 20.12 compatible matrix

For Node 20.12 the maximum working combination is:

| Package                | Version |
|------------------------|---------|
| `vitest`               | `2.1.8` |
| `vite`                 | `6.2.7` |
| `@vitejs/plugin-react` | `4.3.4` |
| `jsdom`                | `25.0.1` |

`vitest 3.x` and `@vitejs/plugin-react 5.x` both require Node 20.19+ and **must not be adopted** until the Node baseline is raised. The original decision and its rationale are in `docs/ADR/adr-0005-node-vitest-version-pinning.md`.

Node engine mismatches surface only during the `yarn install` fetch phase, not at resolve time, so the failure looks like a transient network error — treat any post-bump install failure as a pinning regression first.

## `@types/react` / `@types/react-dom` lockstep

`@types/react-dom` patch releases lag `@types/react`, and an exact matching patch is often not published. Always align both packages to the highest patch version available on DefinitelyTyped for which **both** exist; do not bump one without the other.

A version skew does not fail the build but silently corrupts types such as `ReactNode`, producing downstream type errors in unrelated files.

## Cross-references

- The Vitest migration gotchas (Mock typing, RTL cleanup, vi.mock hoisting, React double-resolution, ESM setup) live in `docs/details/ui-vitest-gotchas.md`.
- The Playwright Java vs Node version skew rule lives in `docs/details/testing-playwright.md`.
- The single-package-manager rule (no coexistence of `package-lock.json` and `yarn.lock`) lives in `.claude/rules/writing-code.md`.
