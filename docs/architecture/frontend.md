# Frontend architecture

The React UI that runs inside the Control Panel portlet. Coding rules for this area are in [`.claude/rules/frontend.md`](../../.claude/rules/frontend.md); unit-test specifics are in [`reference/vitest.md`](../reference/vitest.md).

## Build: esbuild for the bundle, Vitest for tests

Source lives in `src/main/resources/META-INF/resources/js/` (TypeScript + React, no `import React` — the automatic JSX runtime is on). Two independent toolchains use it:

| Purpose | Tool | Entry |
|---|---|---|
| Production bundle | esbuild via `modules/liferay-dummy-factory/scripts/build.mjs` | `yarn build` (run by the Workspace build) |
| Unit tests | Vitest + jsdom | `yarn test` |

`build.mjs` produces what Liferay's loader expects: an ESM bundle at `__liferay__/index.js` with `react`/`react-dom` redirected to the portal's shared copies, an AMD bridge `index.js` that calls `Liferay.Loader.define()`, and `package.json` / `manifest.json` (`{esModule: true, useESM: true}`). `bnd.bnd` `-includeresource` copies these into the JAR.

The two toolchains are deliberately not unified: the bundle must be AMD-compatible for Liferay's loader, while tests only evaluate ESM in jsdom. Moving the bundle to Vite would mean re-implementing the AMD bridge as Vite plugins. `@liferay/npm-scripts` is kept only for `format` / `checkFormat`. Why not `@liferay/npm-scripts` for the build: [ADR-0006](../adr/0006-custom-esbuild-over-npm-scripts.md).

Consequences: no content-hashed filenames, no watch mode / HMR (rebuild the JAR and redeploy), and only `react`/`react-dom` are externalized — importing another portal-global package requires extending the esbuild plugin.

## App structure

| Path | Role |
|---|---|
| `App.tsx` | Two tabs: **Create entities** (per-entity forms) and **Workflow JSON** |
| `config/constants.ts` | `ENTITY_TYPES`, `APP_TABS` |
| `config/entities.ts` | One `EntityFormConfig` per entity: fields, labels, endpoints |
| `components/EntityForm.tsx` | Renders a form from its config, derives `data-testid`s, submits |
| `components/FormField.tsx`, `DynamicSelect.tsx`, `FileUploadArea.tsx` | Field widgets |
| `components/ResultAlert.tsx`, `ProgressBar.tsx` | Result and progress display |
| `components/WorkflowJsonEditor.tsx` and `WorkflowJson*` | Workflow JSON tab |
| `hooks/` | `useFormState`, `useApiData`, `useProgress` |
| `utils/api.ts` | Transport (`postResource`, `postJsonResource`, `parseResponse`) |
| `utils/workflowJsonSchema.ts`, `workflowJsonWorkspace.ts` | Schema loading / Ajv validation, sample workflows |

### Server communication

- Entity forms post to the resource URLs that `view.jsp` passes in `actionResourceURLs`, with `credentials: 'include'`. GET sends parameters in the query string; POST sends `application/x-www-form-urlencoded` with the JSON payload in a `data` parameter.
- Workflow actions post JSON to `/o/ldf-workflow/*`. Every `fetch` to a Liferay `/o/<app>/…` JAX-RS path must send **both** `credentials: 'include'` and `headers: {'x-csrf-token': Liferay.authToken}` (or `?p_auth=`). With the session cookie alone, PortalRealm answers 401 — the on-mount schema fetch then fails silently and every workflow button stays disabled. `test/js/utils/workflowJsonSchema.test.ts` locks both options.
- For non-2xx responses, `parseResponse` returns nonblank string messages from `errors[].message`, joined by newlines, or `Server error: <status>` if the body is empty, non-JSON or has no usable messages. For successful HTTP responses, it treats the payload as failed when `data.success === false || data.error` — both checks, because the two fields are independent. A payload failure passes the full payload through (`ApiResponse<T>` failure carries `data?: T`) so partial batches can be rendered without another request.

### Form field dependencies

`FieldDefinition` has exactly three ways for one field to react to another. Pick one per constraint; do not add a fourth.

| Property | Kind | Behaviour | Example |
|---|---|---|---|
| `dependsOn: {field, paramName}` | data | Options are fetched from `/ldf/data` with the parent's value as a parameter; `DynamicSelect` waits until the parent has a value | categories filtered by the selected site |
| `visibleWhen: {field, value}` | existence | Field is removed from the DOM unless the control matches | structure id only when `createContentsType === '2'` |
| `disabledWhen: {field, value}` | interactivity | Field stays but is disabled when the control matches | `fakerEnable` and `body` disable each other |

Do not combine `visibleWhen` and `disabledWhen` on the same trigger: if a field is meaningless in a state, hide it; if it is meaningful but locked, disable it.

Checkbox state is a **string**: `FormField` calls `onChange(name, String(e.target.checked))`, so `formData.fakerEnable` is `'true'`/`'false'`. `isFieldDisabled` compares with `String(...)` on both sides, so configs may write `value: true`; any new comparator must survive the same round-trip.

### `data-testid` contract

Interactive elements carry mechanically derived ids so Playwright never selects by text. The full contract is in [`reference/test-ids.md`](../reference/test-ids.md).

## i18n: two resolution paths

All display text goes through `Liferay.Language.get(key)`, with keys in `src/main/resources/content/Language.properties`. When a key is missing, `Liferay.Language.get` returns **the key itself** — no warning, no error, the raw kebab-case key appears in the UI. Because the bundle is a custom ESM build, keys reach the browser by two mechanisms:

1. **Serve-time replacement.** Liferay's `LanguageUtil.process()` rewrites `Liferay.Language.get('string-literal')` in served JS. Module keys are only visible to it because `LDFResourceBundleLoader` is registered as a `ResourceBundleLoader` and `bnd.bnd` declares `Provide-Capability: liferay.resource.bundle`. Without that registration, a literal call is replaced by the key.
2. **Runtime cache.** Calls with a variable argument (`Liferay.Language.get(field.label)`) are not matched by the rewrite. For those, `view.jsp` copies `portletConfig.getResourceBundle(locale)` into `Liferay.Language._cache` before rendering the React component. Use `portletConfig.getResourceBundle(locale)`, not `LanguageUtil.get(locale, key)` — the latter only sees portal-global bundles.

Practical rule: prefer passing keys through variables (the entity configs do); a literal is safe only because the `ResourceBundleLoader` is registered. Add the `Language.properties` entry in the same commit as the `Liferay.Language.get` call. Test-side consequences: [vitest.md](../reference/vitest.md#i18n-in-unit-tests) and [playwright.md](../reference/playwright.md#assert-on-resolved-text-never-on-keys).

## Workflow JSON tab

The editor is a thin client of the server contract ([workflow-api](../reference/workflow-api.md)).

- **Schema comes from the server.** `GET /o/ldf-workflow/schema` is fetched on mount and compiled with Ajv (`import Ajv2020 from 'ajv/dist/2020'` — the default `ajv` export does not know draft 2020-12). There is no static schema file. The client must not add required fields the server does not require (e.g. `workflowId` is optional). The TypeScript `WorkflowRequestPayload` in `types/index.ts` is maintained by hand against `WorkflowResource._schemaDocument()`; change both together.
- **Blank until the user loads a sample.** If the editor started pre-filled, the "load sample" action would be a no-op on first render and tests could not observe it. The selected sample is client-side metadata and is never sent.
- **Actions.** *Plan* (`/plan`) and *Execute* (`/execute`) go through the JSON transport (`postJsonResource`), not the form transport, and unit tests click each button so a missing handler import fails in Vitest rather than as a Playwright timeout. Both run Ajv first; there is no separate Validate button. Buttons are disabled while the schema is loading or failed, while a request is in flight, or when the resource URL is missing.
- **One result pane.** Ajv errors and server responses share one `role="status" aria-live="polite"` region with a source badge (`Ajv` / `Plan` / `Execute` / `Load`). Details are collapsed behind a toggle.
- **Live validity.** `liveValidity: 'empty' | 'invalid' | 'ok'` is recomputed 300 ms after typing stops and only drives the textarea's `is-invalid` class (`'empty'` never shows red). Typing clears the current result only when that result came from Ajv.

ARIA carriers (each attribute has one job):

| Element | Attributes |
|---|---|
| Root `div.workflow-json-workspace` | `aria-busy={isBusy}` — only here, never duplicated on inner sections |
| Toolbar | `role="toolbar"` + `aria-label` |
| Textarea | `aria-invalid` (live invalid or Ajv result), `aria-describedby="workflow-json-result-panel"` only while the result is Ajv-sourced, `readOnly` while busy |
| Progress bar | `aria-hidden="true"` (decorative, prevents double announcements) |
| Result pane | `role="status" aria-live="polite"` |
