# Playwright reference

Patterns and pitfalls for the browser specs in `integration-test/`. Selector ids are defined in [`test-ids.md`](test-ids.md). When to use Playwright at all: only when the assertion is about the DOM, client-side validation or navigation — database state is verified through JSONWS ([`.claude/rules/tests.md`](../../.claude/rules/tests.md)).

## Basics

- Headless Chromium through `PlaywrightLifecycle`. Create it as a `@Shared` field in `setupSpec()` and close it with `pw?.close()` in `cleanupSpec()`.
- Log in with `BaseLiferaySpec.loginAsAdmin(pw)` (API POST with CSRF token, as Liferay's own Playwright tests do).
- Open the portlet by direct URL, not through menus:
  `/group/control_panel/manage?p_p_id=<PORTLET_ID>&p_p_lifecycle=0&p_p_state=maximized`
- Give every wait an explicit timeout (`Locator.WaitForOptions().setTimeout(15_000)`, `Page.WaitForURLOptions().setTimeout(30_000)`).
- Browsers are installed by the `installPlaywrightBrowsers` task with the Playwright **Java** CLI, so the browser always matches `test.playwright.version` in `gradle.properties`. Before bumping that property, confirm the version exists on Maven Central (Java releases can lag npm):

  ```bash
  curl -s -o /dev/null -w "%{http_code}" \
      https://repo.maven.apache.org/maven2/com/microsoft/playwright/playwright/<version>/playwright-<version>.pom
  ```

## Selector priority

1. `getByRole(...)` — when the accessible name is not localized, or with role + index.
2. `aria-label` — only when the label is a fixed, non-localized string.
3. `data-testid` — everything else.

Never select by visible text (`getByText`, `:has-text`, `:text-is`): `Liferay.Language.get` makes it locale-dependent, and `:has-text` is a substring match (`categories` also matches `mb-categories`, a strict-mode violation). Decision record: [ADR-0003](../adr/0003-playwright-selector-strategy.md).

## Assert success, not presence

`ResultAlert` renders the same `data-testid="<entity>-result"` element for success and failure; only the class changes (`alert-success` / `alert-danger`). Waiting on the test id alone passes when the server returns an error. Always AND the state class:

```groovy
page.locator('[data-testid="org-result"].alert-success').waitFor(
	new Locator.WaitForOptions().setTimeout(15_000)
)
```

The same applies to `workflow-json-result-panel`.

## Assert on resolved text, never on keys

`Liferay.Language.get('missing-key')` returns `'missing-key'`, so an assertion on the key string passes even when the key is missing from `Language.properties`. Assert on the English value from `Language.properties` (e.g. `"Execution completed successfully."`). A hyphenated string in a text assertion that equals a key name is the smell.

### `textContent()` under `text-transform`

`innerText()` returns rendered text (affected by `text-transform`, pseudo-elements, whitespace collapsing); `textContent()` returns DOM text. The result-source badge is uppercased by CSS, so `innerText()` gives `AJV` while `Language.properties` says `Ajv`. Use `textContent()` for i18n equality checks wherever CSS changes the rendering.

## Waiting correctly

### Async-gated buttons: wait for `:not([disabled])`

Buttons that stay disabled until an on-mount fetch completes (workflow Plan/Execute wait for the schema) must be awaited before clicking, with their own timeout:

```groovy
page.locator('[data-testid="workflow-json-plan"]:not([disabled])')
	.waitFor(new Locator.WaitForOptions().setTimeout(30_000))
page.locator('[data-testid="workflow-json-plan"]').click()
```

Never use `ClickOptions().setForce(true)`: it clicks the disabled button, `onClick` never runs, and the following `waitForResponse` burns its whole timeout while hiding the real bug (the component never left its loading state). Reference: `WorkflowJsonWorkspaceSpec#_runWorkflowAction`.

### `ATTACHED`, not `VISIBLE`, for options and `aria-hidden` elements

`<option>` elements in a collapsed `<select>` and `aria-hidden` elements (e.g. `workflow-json-progress`) are never "visible" to Playwright even when rendered:

```groovy
page.locator("#vocabularyId option[value=\"${id}\"]").waitFor(
	new Locator.WaitForOptions()
		.setState(WaitForSelectorState.ATTACHED)
		.setTimeout(15_000)
)
```

### `Liferay` global is loaded asynchronously

On DXP 2026 the `Liferay` global arrives through the AMD loader after page load; `page.evaluate('() => Liferay.authToken')` right after navigation (even at `NETWORKIDLE`) can throw `ReferenceError`. Poll first:

```groovy
page.waitForFunction(
	"typeof window.Liferay !== 'undefined' && typeof window.Liferay.authToken === 'string'",
	null,
	new Page.WaitForFunctionOptions().setTimeout(15_000)
)
```

`LdfResourceClient._waitForLiferayGlobal` wraps this.

## Workflow JSON tab specs

- Open the tab with `app-tab-workflow-json`; the editor must start empty.
- After `workflow-json-load-sample`, assert that the textarea value changed before waiting on any backend response.
- Assert Plan/Execute through the response or the result panel (with its state class), not just the click.
- Prefer the compact result summary; expand `workflow-json-result-toggle-details` only when the payload matters.
- Keep one small regression check of the entity forms in the same spec so the workflow tab cannot break them unnoticed.
