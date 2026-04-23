# Code Review

L2 layer for PR review and code-quality checks. Observations to apply when reviewing diffs.

## Priority of checks

Review for **contract violations** before "does it work". In this order:

1. **Input boundary policy** — user input is rejected; external data is sanitized; the two are not mixed.
2. **Single source of truth** — no fact appears in more than one file. New rules / contracts land in exactly one file.
3. **Creator pattern compliance** — new batch Creators keep the `TransactionInvokerUtil.invoke` + `throws Throwable` shape.
4. **Batch response contract** — new Creators return `{success, count, requested, skipped, error?, items}`. `success` is `created == requested`, strict.
5. **Test coverage** — both branches of a new `if/else` are tested. RNG-based assertions are paired with deterministic locks.
6. **JSONWS-first** — UI verification only when DOM/rendering is the actual subject under test.

## 1. Input boundary policy

When you see a new user-input path (a new resource-command parameter, a new portlet form field), check:

- If user input: validated and rejected at the resource command or Creator boundary, NOT silently rewritten.
- If external-generated data (Datafaker, RNG, third-party API): sanitized via `ScreenNameSanitizer` or equivalent.
- Strategies are not mixed.

Reference: `UserCreator` `baseName` rejection, `ScreenNameSanitizer` sanitization. Detail in `.claude/rules/writing-code.md`.

## 2. Single source of truth

When a new rule, contract, or gotcha is added:

- Grep for the same content in other files:

	```bash
	grep -r "TransactionInvokerUtil" .claude/rules/ docs/
	grep -r "JSONWS" .claude/rules/ docs/
	```

- If it already exists elsewhere, the PR should **update the existing entry** or link to it — never duplicate.
- Block PRs that introduce duplication.

## 3. Creator pattern

For a new `*Creator` class:

- Method signature is `throws Throwable`.
- Per-entity calls are wrapped in `TransactionInvokerUtil.invoke(_transactionConfig, () -> { ... })`.
- The corresponding `*ResourceCommand` catches `Throwable` (NOT `Exception`) in `doServeResource`.
- Input validation (e.g. `baseName` regex) is thrown **outside** any `invoke(...)` call.

Detail in the Creator pattern section of `.claude/rules/writing-code.md`.

## 4. Batch response contract

For a new Creator or response shape change:

- All of `{success, count, requested, skipped, error?, items}` are returned.
- `success` is `created == requested` (NOT `created > 0`).
- Whenever `success == false`, `error` is set. There is an **unconditional `else` branch** inside the `if (!success)` block.
- `requested` and `skipped` are not omitted.
- The frontend `parseResponse` checks `data.success === false || data.error` (both, not just one).

## 5. Test coverage

### Branch coverage

For a new `if`/`else`:

- Each branch has at least one independent feature method exercising it.
- Example: a new `if (fakerEnable)` split with different sanitize/validate logic must have both `fakerEnable=true` and `fakerEnable=false` paths under test, with happy and sad cases.

### Deterministic locks for RNG-based tests

A test that consumes RNG or faker output:

- The "looks right" assertion alone fails only probabilistically on regression.
- It is paired with a **deterministic regex lock**:

	```groovy
	(response.users as List).every {
	    (it.screenName as String) ==~ /^[a-z0-9._-]+$/
	}
	```

Detail in the Test Design section of `.claude/rules/testing.md`.

### Response shape locks

When the response shape is part of the contract, tests assert **presence and type** of every field on both success and failure paths — not just the values seen in the happy case. A `response.containsKey('skipped')` line is expected.

## 6. JSONWS-first

For new integration tests:

- Post-condition checks (DB state) go through JSONWS, not Playwright UI navigation.
- If Playwright is used, there's a stated reason (DOM/rendering check, navigation flow). Otherwise, propose switching to JSONWS.

Detail in the Verification Strategy section of `.claude/rules/testing.md`.

## Documentation consistency

If the diff touches any of:

- Java API signatures → check `docs/details/api-liferay-ce74.md`
- Playwright patterns → check `docs/details/testing-playwright.md`
- Version pins → check `docs/details/dependency-policy.md`
- Architectural decisions → check `docs/ADR/`
- New cross-cutting rules → check `CLAUDE.md` and the relevant L2 file

Especially: changes to `gradle.properties`, `package.json`, `yarn.lock`, or `Language.properties` should come with documentation updates in the same PR.

## Anti-patterns to flag

- **"It works, ship it"** — contract violations are regressions even when the test stays green.
- **"For future use"** — abstractions added without a current consumer are rejected.
- **"Just-in-case try/catch"** — error handling around internal code is rejected. Validate at boundaries (user input, external API), not internally.
- **"Add a comment to clarify"** — if the identifier is self-explanatory, the comment is noise. Comments are for non-obvious WHY only.
- **`as any` / `as unknown as`** — escape hatches require justification. The minimal-shape mock pattern in tests is the documented exception (see `.claude/rules/testing.md`).
- **Hyphenated string in a localized assertion** — code smell that the assertion is matching the i18n key, not the resolved value. Look up the real `Language.properties` value.
