# ADR-0003: Playwright Selector Strategy: data-testid with getByRole Fallback

## Status

Accepted

## Date

2026-04-11

## Context

The integration test suite uses Playwright to drive the Control Panel portlet through a real browser. Historically, locators were written against visible text — `page.locator('button:has-text("Create")')`, `page.getByText('Organization created')`, `page.locator('.nav-link:text-is("categories")')`. These selectors worked while the UI was English-only.

Introducing `Liferay.Language.get(...)` for every user-visible string made the UI locale-dependent. The same key resolves to `"Create"` in English and `"作成"` in Japanese, so any spec that waits on a literal substring becomes brittle the moment the default locale changes — locally, in CI, or after a Liferay upgrade that flips the admin's language. A spec that greps the DOM for `"Create"` is not asserting "the create button was clicked"; it is asserting "the container happens to run in a locale where the word *Create* exists in this position".

On top of that, `:has-text()` is a substring match, which already caused at least one strict-mode violation (`.nav-link:has-text("categories")` matched both the *categories* tab and the *mb-categories* tab). The text-based approach has structural problems that i18n only made worse.

We needed a selector strategy that (a) does not depend on copy, (b) survives i18n changes, (c) does not collide when multiple components share a label, and (d) is still discoverable by developers reading React source.

## Decision

We adopt a layered strategy, modeled after the Testing Library priority ladder:

1. **`getByRole(...)`** — first choice. Matches the accessibility tree and is both i18n-stable (role, not text) and semantically meaningful. Use `getByRole('button', { name: ... })` only when `name` is a stable, i18n-independent identifier; otherwise rely on role + index or role + testId.
2. **`aria-label`** — second choice, but only when the label value is NOT an i18n key. If the label is passed through `Liferay.Language.get(...)` it has the same drift problem as visible text and is unsuitable.
3. **`data-testid`** — fallback for everything else. Always allowed as an explicit escape hatch when role + accessible name cannot uniquely identify the element (e.g. two submit buttons on the same screen, custom Clay components that render without a semantic role, result alerts).

### Naming convention

- kebab-case, domain-term first, role suffix last
- Good: `user-count-input`, `organization-create-submit`, `role-type-select`, `mb-thread-result`
- Bad: `btn1`, `submit-2`, `Form__submit`, `myTestId`

### Generation contract

`EntityForm.tsx` derives `entityKey` from the entity type (`ORG` → `org`, `MB_THREAD` → `mb-thread`), and form element ids are built as `${entityKey}-${kebab(fieldName)}-${typeSuffix}`. Submit and result ids are `${entityKey}-submit` and `${entityKey}-result`. See `.claude/rules/writing-code.md` for the full rule.

### Placement scope

Only elements that Playwright actually interacts with receive a `data-testid`:

- interactive inputs (text, number, select, multiselect, textarea, file, checkbox)
- buttons (submit, reset, action)
- result/alert regions
- tabs and step navigation

Links, icons, decorative wrappers, labels, and purely visual elements do NOT receive a testId. This keeps the DOM clean and prevents the testId from becoming a general-purpose dev artifact.

### Reusable component collision

Generic components (`FormField`, `DynamicSelect`, `ResultAlert`) accept an optional `testId?: string` prop and emit `data-testid={testId}` only when the prop is defined. Parents pass concrete ids (e.g. `<FormField testId="user-count-input" />`), so the same component instance can appear on multiple screens without id collisions and without hard-coding a name inside the generic component.

### Migration strategy

Big-bang: React components received testIds first, then every `*FunctionalSpec` under `integration-test/src/test/groovy/.../spec/` was rewritten in a single PR. No mixed locators across specs — a spec either uses the new strategy entirely, or it has not been migrated yet.

## Alternatives Considered

- **`data-testid` only, no `getByRole`** — rejected. Breaks from the Testing Library priority ladder; loses the a11y-first signal; encourages developers to invent ids for elements that already have a stable role + name.
- **Keep `getByText` / `:has-text()` / `:text-is()`** — rejected. i18n breaks all three; substring matching additionally causes strict-mode collisions (see the `categories` vs `mb-categories` tab incident documented in `.claude/rules/testing.md`).
- **`aria-label` everywhere** — rejected. `aria-label` is itself run through `Liferay.Language.get(...)` for accessibility, so the value drifts with locale exactly like visible text does. It is usable only when the label is a stable non-i18n identifier.
- **XPath / CSS descendant chains** — rejected. Brittle to DOM refactors; impossible to grep from spec to React source.

## Consequences

### Positive

- Spec locators survive locale changes, copy edits, and component re-styling.
- New React components carry an implicit testing contract: if Playwright touches it, it has a `testId?` prop.
- The generation contract in `EntityForm.tsx` means most ids are mechanical and predictable; engineers do not have to grep for the current id of a field they just added.
- `getByRole` usage keeps the a11y tree exercised by tests, catching some accessibility regressions for free.

### Negative

- Every interactive React component must plumb a `testId?: string` prop through its API. This is boilerplate.
- Adding a new field to an entity form is a two-step change: update the React field config **and** update any spec that iterates fields by testId.
- Existing specs written against `has-text` had to be rewritten in bulk; partial migration was explicitly not allowed.
- `data-testid` attributes ship to production. We accept this because the byte cost is negligible and the alternative (stripping them at build time) adds a toolchain concern.

## References

- `.claude/rules/writing-code.md` — `## JavaScript / React` section (Playwright selector strategy, testId naming, entityKey derivation, generation contract)
- `.claude/rules/testing.md` — `## Playwright Success Assertion Pattern`, `## Playwright / Headless Gotchas`
- `.claude/plan/improve_playwright_test.md` — original migration plan and discussion notes
