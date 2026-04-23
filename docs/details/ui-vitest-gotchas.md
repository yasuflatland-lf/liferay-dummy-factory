# Vitest migration gotchas

L3 detail. Source of truth for Jest → Vitest migration pitfalls. Easy to get wrong when mechanically porting Jest specs; most failures are silent (tests pass on a regression, or whole files are skipped without counting as failures). Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

## B. Vitest-native semantics (differences from Jest)

### `Mock<T>` generic-argument order

Jest uses `Mock<TReturn, TArgs>`; Vitest 2.x uses `Mock<T extends Procedure>` and takes a **single function type**. Porting the Jest form compiles but silently disables type checking on the mock. The correct form is:

```ts
const fetchMock = vi.fn() as Mock<(input: RequestInfo, init?: RequestInit) => Promise<Partial<Response>>>;
```

### Set `globals: false` in `vitest.config.ts`

This forces `describe / it / expect / vi` to be imported from `'vitest'` in every spec, avoiding namespace pollution and giving reliable type inference. Do not rely on Jest-style globals.

### React Testing Library 16 does NOT auto-cleanup under Vitest

Unlike Jest + RTL, Vitest does not trigger RTL's auto-cleanup even with `globals: false`. Register it manually in `setupFiles`:

```ts
import {afterEach} from 'vitest';
import {cleanup} from '@testing-library/react';
afterEach(() => cleanup());
```

Without this, DOM from previous tests persists and `queryByText` / `getByRole` fail with `Found multiple elements`.

### `vi.mock` factory hoisting

`vi.mock(...)` is hoisted above imports. If the factory references an outer variable, you get a `ReferenceError` and the entire spec file is reported as a **load error**, which Vitest surfaces as a silent skip in some reporters. Use `vi.hoisted` to declare shared state:

```ts
const {mockFetch} = vi.hoisted(() => ({mockFetch: vi.fn()}));
vi.mock('../src/api', () => ({fetch: mockFetch}));
```

## C. Vite React double-resolution

### Do not use `resolve.alias` regexes for `react/*` subpaths

A pattern like `^react\/(.*)$` → `${reactDir}/$1` breaks Vite's extension resolution for `react/jsx-dev-runtime`, `react/jsx-runtime`, etc. — Vite treats the alias target as a fully resolved path and does not append `.js`, so the import fails at load time.

### `resolve.dedupe` is the correct fix

Use:

```ts
resolve: {dedupe: ['react', 'react-dom']}
```

This collapses duplicate copies of React without touching subpath resolution, and normal `node_modules` resolution still handles `react/jsx-dev-runtime`.

### Do not mechanically translate Jest's `moduleNameMapper` into Vite aliases

Most `moduleNameMapper` entries aimed at React duplication should become `resolve.dedupe` entries, not `resolve.alias` rewrites.

## D. ESM `setup.ts` details

### `__dirname` is not defined in ESM

Vitest evaluates `setup.ts` as ESM, so CommonJS globals are gone. Use `import.meta.dirname` (Node 20.11+) to locate sibling resource files like `Language.properties`.

### Avoid `fileURLToPath(import.meta.url)`

Under Vite's transform pipeline it can fail with `fileURLToPath is not a function` depending on how the setup file is bundled. `import.meta.dirname` is native and reliable.

### Use `globalThis`, not `global`

Jest's jsdom environment exposed `global`, but Vitest + ESM expects assignments like `globalThis.Liferay = {...}`. The Jest pattern compiles but leaves the stub unreachable from production code.

## E. `@testing-library/react@16` with React 19

- RTL 16 is the first version with React 18 / 19 dual peer support. RTL 14.x pins a React 18 peer and will conflict when the module is upgraded to React 19.
- `@testing-library/jest-dom` works under Vitest, but the matchers are only registered when `test/setup.ts` does `import '@testing-library/jest-dom/vitest'` (note the `/vitest` subpath). The plain `@testing-library/jest-dom` import is a no-op under Vitest. If no spec uses the custom matchers, drop the dependency entirely instead of importing the wrong entry point.

## Cross-references

- The general dependency-pinning policy for `vite` / `vitest` / `@vitejs/plugin-react` / `jsdom` lives in `docs/details/dependency-policy.md`.
- The unit-test side of the i18n fallback contract (`languageMap` setup, fallback guard) lives in `.claude/rules/testing.md`.
