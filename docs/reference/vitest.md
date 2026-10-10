# Vitest reference

Frontend unit tests: `modules/liferay-dummy-factory/test/**/*.test.{ts,tsx}`, configured in `vitest.config.mts`, global setup in `test/setup.ts`. Run them with `yarn test` in the module directory. Most mistakes here are **silent** — tests pass on a regression, or a file is skipped without counting as a failure.

## `test/setup.ts`

- **Loads the real `Language.properties`** synchronously (`readFileSync`) when the module loads, and stubs `globalThis.Liferay.Language.get` as `languageMap.get(key) ?? key`. Tests see resolved values with no build step. Sync I/O is intentional: setup files run before any test module, and an async load would need `beforeAll` plumbing in every spec.
- The parser skips blank and `#` lines and splits on the **first** `=` so values containing `=` survive. Do not change it to `split('=')`.
- Stubs `Liferay.authToken` as `'test-auth-token'`.
- Registers `afterEach(() => cleanup())`. React Testing Library does **not** auto-clean under Vitest; without this, DOM leaks between tests and queries fail with "Found multiple elements".
- It is ESM: use `import.meta.dirname` (not `__dirname` or `fileURLToPath(import.meta.url)`, which can fail under Vite's transform) and assign globals on `globalThis`, not `global`.

`setup.ts` is the only shared test file. Helpers (render wrappers, fixture builders, mock factories) stay in the spec that uses them; if two specs need one, copy it.

## i18n in unit tests

Because the stub falls back to the key, a missing key does not fail anything by itself: the component renders the key and an assertion that happens to compare against it still passes. Every assertion on a localized string pairs the positive check with a guard that the key resolved:

```ts
const text = Liferay.Language.get('create-user');

expect(text).not.toBe('create-user');
expect(text.length).toBeGreaterThan(0);
```

Deleting the key from `Language.properties` then fails the test instead of echoing the key.

## Configuration rules (`vitest.config.mts`)

- `globals: false` — import `describe`, `it`, `expect`, `vi` from `'vitest'` in every spec.
- `resolve.dedupe: ['react', 'react-dom']` collapses duplicate React copies. Do not use a `resolve.alias` regex for `react/*`: Vite treats the target as fully resolved and `react/jsx-dev-runtime` fails to load. Jest-era `moduleNameMapper` entries for React become `dedupe` entries, not aliases.
- `@testing-library/jest-dom` matchers register only via `import '@testing-library/jest-dom/vitest'`; the plain import is a no-op. The project currently does not use them.

## Mocking

### Hooks: minimal shape

When a component under test calls a custom hook, mock the hook and return only the fields the component reads:

```ts
import {vi, type Mock} from 'vitest';

const mockedUseFormState = useFormState as unknown as Mock<typeof useFormState>;

mockedUseFormState.mockReturnValue({
	formData: {count: 1, baseName: 'Test'},
	handleChange: vi.fn(),
} as unknown as ReturnType<typeof useFormState>);
```

Replicating the full return shape couples every spec to every hook field. This `as unknown as` is the documented exception to the "no escape hatches" rule.

### `Mock<T>` takes one function type

Vitest's `Mock<T extends Procedure>` takes a single function type (Jest's `Mock<TReturn, TArgs>` order compiles but silently disables type checking):

```ts
const fetchMock = vi.fn() as Mock<
	(input: RequestInfo, init?: RequestInit) => Promise<Partial<Response>>
>;
```

### `vi.mock` is hoisted

A `vi.mock` factory that references an outer variable throws `ReferenceError`; some reporters show the whole file as skipped. Declare shared mocks with `vi.hoisted`:

```ts
const {mockFetch} = vi.hoisted(() => ({mockFetch: vi.fn()}));

vi.mock('../src/api', () => ({fetch: mockFetch}));
```

## Locking contracts in unit tests

- **JAX-RS fetch auth.** Every new `fetch` to `/o/<app>/` gets a test asserting `init.credentials === 'include'` and `init.headers['x-csrf-token'] === Liferay.authToken` (reference: `test/js/utils/workflowJsonSchema.test.ts`).
- **TS ↔ Groovy JSON fixture parity.** When TypeScript code and a Spock spec must use identical JSON, the fixture file under `integration-test/src/test/resources/<domain>/` is the single source. The Spock spec loads it from the classpath; a Vitest test reads it with `readFileSync` and asserts `expect(JSON.parse(tsValue)).toEqual(JSON.parse(fixture))` (deep-equal: key order is free, names, array order and field count are locked). Canonical instance: `workflowJsonWorkspace.parity.test.ts` for `workflow-samples/`.
