# Playwright — concrete details

L3 detail. Source of truth for Playwright-specific selector patterns, version pinning, and headless gotchas. Read on demand from `.claude/rules/testing.md` or `.claude/rules/debugging.md`.

## Browser tests

- Runs **headless Chromium** via `PlaywrightLifecycle`.
- Login credentials: `test@liferay.com` / `test` (Liferay default admin).
- Navigate to portlets via **direct URL** with the portlet ID in the query string (`p_p_id=...&p_p_lifecycle=0`). Do not click through menus — direct navigation is faster and more reliable.
- Use CSS selectors for locators (`#count`, `.alert-success`, `button.btn-primary`, `[type=submit]`).
- Set explicit timeouts on waits: `waitForURL(..., new Page.WaitForURLOptions().setTimeout(30_000))`, `waitFor(new Locator.WaitForOptions().setTimeout(15_000))`.
- Close the `PlaywrightLifecycle` instance in `cleanupSpec()` using safe-navigation: `pw?.close()`.

## Playwright Java vs Node version skew

Playwright Java (`com.microsoft.playwright:playwright` on Maven Central) and Playwright Node/CLI (`playwright` on npm) ship on **separate release cycles**. The same `1.x.y` number can exist on one side and not the other.

As of 2026-04, npm publishes `1.59.1` as the latest stable, while Maven Central's latest is `1.59.0` — `1.59.1`, `1.59.2`, and `1.60.x` all return HTTP 404 from the Maven repo.

The Playwright project recommends keeping the **client (Java) and driver (CLI) on the same version**, so bumping one side ahead of the other invites protocol skew and must be avoided.

Before bumping the Java side, **always confirm the version actually exists on Maven Central** with a direct POM fetch:

```bash
curl -s -o /dev/null -w "%{http_code}" \
    https://repo.maven.apache.org/maven2/com/microsoft/playwright/playwright/<version>/playwright-<version>.pom
```

`200` means the artifact is published; `404` means it is not yet available. Do **not** rely on the Maven Search API (`search.maven.org/solrsearch`) for this check — its index lags behind the repo and will miss recent releases. The direct URL is authoritative.

`gradle.properties`' `test.playwright.version` and the workflow's `npx playwright@<version>` invocation must be **pinned to the same version in the same commit**. Never update one without updating the other.

The original decision and its rationale are in `docs/ADR/adr-0004-github-actions-version-policy.md`.

## Success-assertion tautology pattern

`ResultAlert` emits the same `data-testid="<entity>-result"` regardless of state (success / danger / warning) because the alert region is one element whose class flips between `alert-success` and `alert-danger`. Waiting on the testId alone therefore also passes on failure — a tautology that has been shipped and caught in review.

The correct pattern AND-s the success class onto the `data-testid` selector:

```groovy
page.locator('[data-testid="organization-result"].alert-success').waitFor(
    new Locator.WaitForOptions().setTimeout(15_000)
)
```

Never write `page.locator('[data-testid="organization-result"]').waitFor(...)` as a post-condition for a "create succeeded" assertion. If the server returns an error, the alert still appears, the wait still resolves, and the test goes green on a regression.

## Async-gated buttons: wait for `:not([disabled])`, never `setForce(true)`

When a button's readiness depends on an on-mount async fetch (the Workflow JSON schema load is the canonical example — Plan/Execute stay disabled until `schemaStatus === 'ready'`), the test MUST wait for the button to transition out of `disabled` before clicking:

```groovy
page.locator(
    "[data-testid=\"${WORKFLOW_JSON_PLAN_TEST_ID}\"]:not([disabled])"
).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
page.locator(
    "[data-testid=\"${WORKFLOW_JSON_PLAN_TEST_ID}\"]"
).click()
```

Do NOT use `click(new Locator.ClickOptions().setForce(true))` as the workaround. `setForce(true)` bypasses Playwright's actionability checks, so the click lands on a disabled button, the button's `onClick` never fires (because the DOM still respects `disabled`), and the subsequent `waitForResponse` hangs for the full timeout — exposing itself as a slow test but hiding the real bug (the component never left its loading state).

Giving the `:not([disabled])` wait its own timeout separate from `waitForResponse` also gives each phase an independent budget — a slow schema fetch fails early with a clear locator message instead of being mis-attributed to a slow action endpoint.

Reference: `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/WorkflowJsonWorkspaceSpec.groovy#_runWorkflowAction`.

## `<option>` elements need `ATTACHED` state, not `visible`

When waiting for an `<option>` inside a collapsed `<select>`, Playwright's default `visible` state treats it as hidden and the wait times out even though the element exists in the DOM. Use `WaitForSelectorState.ATTACHED`:

```groovy
page.locator("#vocabularyId option[value=\"${id}\"]").waitFor(
    new Locator.WaitForOptions()
        .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
        .setTimeout(15_000)
)
```

This applies to every cascading dropdown verification (category/vocabulary/thread pickers).

## `:has-text()` is substring; `:text-is()` is exact

`.nav-link:has-text("categories")` matches BOTH `categories` AND `mb-categories` tabs and triggers a Playwright strict-mode violation. When an entity label is a substring of another label, always use `:text-is()`:

```groovy
page.locator('.nav-link:text-is("categories")').click()
```

## Headless Delivery `?search=` goes through Elasticsearch

`/o/headless-delivery/v1.0/.../message-board-sections?search=...` hits the Elasticsearch index, which has ingestion latency. Tests running immediately after creation will get 0 results. For post-condition verification, drop `?search=` and fetch the full list with `?pageSize=100`, then filter client-side on `title` / `name`:

```groovy
def response = headlessGet(
    "/o/headless-delivery/v1.0/sites/${siteId}/message-board-sections?pageSize=100"
)
def matching = response.items.findAll { it.title?.startsWith(BASE_NAME) }
```

The `message-board-sections` listing endpoint is DB-backed (not ES-backed), so there is no indexing lag.

## DXP 2026 deferred module bootstrap — `Liferay.authToken` is not synchronous

`page.evaluate('() => Liferay.authToken')` immediately after `page.goto(...)` fails on DXP 2026 with `ReferenceError: Liferay is not defined`, even though the DOM has rendered and `LoadState.NETWORKIDLE` has been reached. DXP 2026 loads the `Liferay` global through the Liferay AMD module loader, which resolves asynchronously after page load — the standard Playwright readiness states do not wait for it.

Always poll for the global before reading it:

```groovy
page.waitForFunction(
    "typeof window.Liferay !== 'undefined' && " +
        "typeof window.Liferay.authToken === 'string'",
    null,
    new Page.WaitForFunctionOptions().setTimeout(15_000)
)

String authToken = page.evaluate('() => Liferay.authToken') as String
```

Wrap this in a `_waitForLiferayGlobal(Page page)` helper if you call it more than twice — `LdfResourceClient.groovy` does exactly that. The same applies to anything else exposed on the `Liferay` namespace (`Liferay.Session`, `Liferay.Util`, etc.).

## Headless API vs Java API names for the same Message Boards entities

The IDs match between layers, so you can create with one API and verify/delete with the other.

| Java API     | Headless API             |
|--------------|--------------------------|
| `MBCategory` | `message-board-section`  |
| `MBThread`   | `message-board-thread`   |
| `MBMessage`  | `message-board-message`  |

## Workflow JSON workspace E2E

When testing the workflow JSON workspace, prefer stable `data-testid` locators over visible copy or proxy controls.

### What to assert

- The workspace opens after selecting `Workflow JSON`.
- The editor starts blank unless the user explicitly loads a sample.
- Clicking the in-editor sample loader changes the textarea value.
- `plan` / `execute` should be asserted through the response path, not only through button clicks. There is no separate `validate` button — client-side Ajv validation runs automatically before every action, and `/plan` is the only server validation endpoint.
- Legacy entity forms still need a small regression check in the same spec so the new workspace does not break the old shell.

### Why this pattern

- Text labels change more often than test ids.
- A preloaded sample can make the sample-loader appear to work while actually doing nothing.
- Waiting on `response` or a result panel is safer than waiting on button presence alone.

### Selector guidance

- Use stable ids like `app-tab-workflow-json`, `app-tab-create-entities`, `workflow-json-textarea`, `workflow-json-load-sample`, and `workflow-json-result-toggle-details`.
- Avoid introducing proxy selectors in the shell when the real control already exists in the workspace.
- If a click is supposed to change the editor value, assert the value change directly before waiting on the backend response.
- Keep the workflow JSON workspace blank until a sample is loaded explicitly.
- When verifying results, prefer the compact summary first and only expand the details panel when the payload itself matters.

### Current testids in the workflow JSON workspace

- `workflow-json-toolbar` — upper-toolbar `<div role="toolbar">` wrapping the Plan and Execute buttons. Sits above the `<section class="sheet">` so `closest('section')` from either button would miss it; traversing `closest('[aria-busy]')` from the execute button resolves to the outer `.workflow-json-workspace` div, which is the authoritative `aria-busy` carrier.
- `workflow-json-plan` / `workflow-json-execute` — action buttons inside the toolbar; disabled when the corresponding resource URL is absent, **while the initial schema fetch is in flight** (`schemaStatus === 'loading'`) or failed (`'error'`), or while an action is in-flight. The former `workflow-json-validate` testid is gone: Plan and Validate hit the same `/plan` endpoint, so Validate was redundant surface.
- `workflow-json-result-panel` — `<section role="status" aria-live="polite" id="workflow-json-result-panel">`. Textarea `aria-describedby` points at this `id` only when the latest result's `action === 'ajv'`. Continue asserting by AND-ing `.alert-success` or `.alert-danger` onto the selector (see success-assertion tautology pattern above).
- `workflow-json-result-source` — badge inside the result pane showing the origin of the result (`Ajv` / `Plan` / `Execute` / `Load`). The `.workflow-json-source` SCSS rule applies `text-transform: uppercase`, so Playwright's `innerText()` returns the rendered uppercase form while `textContent()` returns the raw Language.properties value. Assert source labels via `textContent()` — see "`innerText()` vs `textContent()` under text-transform" below.
- `workflow-json-result-placeholder` — empty-state filler shown when `result === null`. Replaced by the result panel once any action completes.
- `workflow-json-progress` — decorative progress bar marked `aria-hidden="true"`. Use `WaitForSelectorState.ATTACHED` rather than `VISIBLE`: Playwright's visibility heuristic can treat `aria-hidden` elements as not visible even when they are rendered.

	```groovy
	page.locator('[data-testid="workflow-json-progress"]').waitFor(
	    new Locator.WaitForOptions()
	        .setState(WaitForSelectorState.ATTACHED)
	        .setTimeout(10_000)
	)
	```

**Anti-pattern:** asserting the progress bar by `.isVisible()` or waiting with default `VISIBLE` state — the bar is `aria-hidden` and will return false even when rendered. Always use `ATTACHED`.

Reference: `integration-test/src/test/groovy/com/liferay/support/tools/it/spec/WorkflowJsonWorkspaceSpec.groovy`.

## `innerText()` vs `textContent()` under `text-transform`

`Locator.innerText()` returns the **rendered visible text** — it respects CSS `text-transform`, `::before`/`::after` content, and whitespace collapsing. `Locator.textContent()` returns the **raw DOM text** — unaffected by CSS.

When a selector target has `text-transform: uppercase` (or `lowercase`, or `capitalize`) applied via SCSS, `innerText()` reads the transformed form while Language.properties stores the original case. A test written as

```groovy
assert badge.innerText() == 'Ajv'
```

fails because the rendered text is `AJV`. The fix is to read the untransformed text directly:

```groovy
assert badge.textContent() == 'Ajv'
```

`.claude/rules/testing.md` requires asserting on the **resolved English phrase from Language.properties**. `textContent()` returns exactly that phrase; `innerText()` returns whatever the CSS pipeline produces on top. For i18n equality checks, always prefer `textContent()` when a `text-transform` is present upstream.

The same applies to `letter-spacing`, `::first-letter` tweaks, and `white-space` collapsing — anywhere visible text diverges from raw DOM text.
