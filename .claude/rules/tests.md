---
paths:
  - "integration-test/**"
  - "modules/liferay-dummy-factory/src/test/**"
  - "modules/liferay-dummy-factory/test/**"
---

# Test rules

How to run tests: `docs/guides/testing.md`. Harness internals: `docs/reference/test-harness.md`. Browser patterns: `docs/reference/playwright.md`. Vitest: `docs/reference/vitest.md`.

## What to test

- **Pure logic gets a host-JVM JUnit test first** (seconds, no container). Integration specs prove the integration — that the branch is wired, JSON flows end to end, and Liferay accepts the result — not the inner logic.
- **Every branch of a new `if/else` gets its own feature method**, happy and sad path (e.g. both `fakerEnable=true` and `false`, plus a dirty-input rejection such as `baseName: "O'Brien"`).
- **RNG/faker output gets deterministic locks**: a regex every item must match (`==~ /^[a-z0-9._-]+$/`), and for per-item random values an all-distinct lock (`(items*.body as Set).size() == count`).
- **Lock the response shape, not just values**: assert presence (`containsKey`) of `success`, `count`, `requested`, `skipped`, `items` on success and failure paths. `error` is absent on success — never require it there.
- **Wire-format renames**: sweep both dotted access (`\.users\b`) and string literals (`"users"`) across `integration-test/`.

## Integration specs (Spock)

- Live in `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/`, extend `BaseLiferaySpec`, call `ensureBundleActive()` in `setupSpec()`. `@Stepwise` when order matters.
- **Verify database state through JSONWS** (`jsonwsGet/Post` with path suffixes only). Use Playwright only when the DOM, client-side validation or navigation is the subject.
- Before using a JSONWS endpoint for verification or cleanup, check it is a remote `*Service` and not blacklisted (`docs/reference/liferay-dxp-api.md`).
- **Identity lock first**: prove the JSONWS response is the entity you asked for (e.g. `emailAddress.equalsIgnoreCase(email)`) before asserting other fields.
- **Diagnostic asserts first** in `then:` blocks: `assert response.error == null : "creator failed: ${response}"` before bare boolean conditions on the same data.
- **`@Stepwise` shared fields**: guard before use (`assert !apiResponseBody.isEmpty() : 'prior feature method did not run…'`).
- **Cleanup**: if an entity has no working delete path, skip cleanup with a one-line comment saying why. No best-effort cleanup that swallows errors.
- Spock style: `def 'describes behaviour'()`, `given/when/then/expect`, `?.`, numeric literals with underscores (`30_000`).
- Verify compilation with `:integration-test:compileTestGroovy` (not `compileGroovy`).

## Playwright

- Selectors: `getByRole` → non-localized `aria-label` → `data-testid`. Never visible text.
- Success waits AND the state class: `[data-testid="org-result"].alert-success`.
- Async-gated buttons: wait for `:not([disabled])`; never `setForce(true)`.
- Text assertions use the resolved English value from `Language.properties`, never the key; use `textContent()` under CSS `text-transform`.
- Explicit timeouts on every wait; `pw?.close()` in `cleanupSpec()`.

## Vitest

- See `.claude/rules/frontend.md` (unit tests section).
