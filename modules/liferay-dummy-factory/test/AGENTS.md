# Frontend unit tests (Vitest)

Scope: `modules/liferay-dummy-factory/test/`. Also obey [`../AGENTS.md`](../AGENTS.md) (module) and the root [`AGENTS.md`](../../../AGENTS.md). The code under test follows [`src/main/resources/AGENTS.md`](../src/main/resources/AGENTS.md). Setup and patterns: [`docs/reference/vitest.md`](../../../docs/reference/vitest.md).

Run: `(cd modules/liferay-dummy-factory && yarn test)`.

- Import test APIs from `'vitest'` (`globals: false`). Helpers stay in the spec file; only `test/setup.ts` is shared.
- Pair every localized-string assertion with the fallback guard: `expect(text).not.toBe(key)`. Never assert a hyphenated key-like string as display text.
- Mock hooks with the minimal-shape pattern (`as unknown as ReturnType<typeof hook>`), `Mock<FnType>` with a single function type, shared mock state via `vi.hoisted`.
- JSON shared with Spock specs lives in one fixture under `integration-test/src/test/resources/`, locked by a deep-equal parity test.
