# Liferay Workspace frontend traps

L3 detail. Non-obvious pitfalls hit while wiring the React frontend and its unit-test stack into a Liferay Workspace module. Read on demand from `.claude/rules/writing-code.md`.

## J25: `@liferay/npm-scripts` bundles its own `@testing-library/react@14`

Even when the module's `package.json` pins `@testing-library/react@16.3.0` at the top level, npm emits an `incorrect peer dependency "react@^18.0.0"` warning because `@liferay/npm-scripts` brings in a transitive copy of v14 along with its own React 18 testing stack.

The warning is **harmless** and can be ignored — the top-level v16 wins for the module's own Vitest runs, and the bundled v14 copy is only used by `@liferay/npm-scripts` internals. Do not attempt to "resolve" the warning by downgrading the top-level pin or by mucking with `resolutions`/`overrides`; both make the actual test stack inconsistent with the React version the production bundle ships.

## J26: `esbuild` build path is independent of Vite/Vitest

`scripts/build.mjs` uses `esbuild` plus an AMD Loader bridge to produce the Liferay-compatible bundle, while unit tests run under Vitest. These two toolchains do **not** need to be unified — shipping a production build via esbuild while running tests via Vitest is a supported split.

Do NOT attempt to collapse them onto a single bundler just for consistency. The constraints are different:

- The production bundle must be AMD-compatible for Liferay's loader, which esbuild + the AMD bridge handles cleanly.
- The Vitest run only needs to evaluate ESM modules in jsdom; it never produces an output bundle.

Forcing both onto Vite for "consistency" requires re-implementing the AMD bridge inside Vite plugins, which is strictly more code and more failure modes than the split design.

## J27: `LanguageUtil.process()` replaces string-literal `Liferay.Language.get()` calls at serve-time

**Why:** Liferay's resource serving pipeline applies `LanguageUtil.process()` to JS files, which regex-replaces `Liferay.Language.get('string-literal')` with the resolved value. For module-specific keys not registered via `ResourceBundleLoader`, the key itself becomes the literal — silently breaking i18n. Variable-parameter calls (`Liferay.Language.get(variable)`) are not matched by the regex and survive to runtime.

**What:** Never hardcode module-specific i18n keys as string literals in component code. Pass keys via variables (e.g. `Liferay.Language.get(field.label)` instead of `Liferay.Language.get('upload-template-files')`). If string literals are unavoidable, register a `ResourceBundleLoader` component (`LDFResourceBundleLoader.java`) with `Provide-Capability: liferay.resource.bundle` in `bnd.bnd` to make the module's `Language.properties` globally visible to `LanguageUtil`.

## J28: New `MVCResourceCommand` endpoints require `view.jsp` registration

**Why:** The React frontend resolves resource URLs from `<portlet:resourceURL>` tags rendered in `view.jsp`. Without registration, `LdfResourceClient` (integration tests) and the frontend `postResource` function cannot find the endpoint URL, producing "Could not find resource URL" errors.

**What:** When adding a new `MVCResourceCommand` (e.g. `/ldf/blog`), add both entries to `view.jsp` in the same commit:
1. `<portlet:resourceURL id="/ldf/blog" var="blogResourceURL" />`
2. `actionResourceURLs.put("/ldf/blog", blogResourceURL)` in the HashMap initialization

## J29: Workflow JSON workspaces should start empty, not preloaded

**Why:** If the editor starts with the first sample already in place, the "load sample" action becomes a no-op on first render. That hides the effect of the control and makes E2E flows wait for a value change that never happens.

**What:** For sample-driven workspaces, keep the editor blank on first render and let the user load a sample explicitly. Store the selected sample separately from the textarea value so sample metadata stays client-side.

## J30: Workflow JSON preflight should match the backend schema, not the UI's assumptions

**Why:** The backend accepts workflow requests without `workflowId`, so a client-side validator that requires it rejects valid payloads and blocks `/plan` and `/execute` unnecessarily. The same trap exists for any field: adding a client-only `required` entry in the schema causes valid server payloads to be rejected silently.

**What:** The workflow JSON schema is now fetched dynamically from `GET /o/ldf-workflow/schema` at component mount time. The server is the single source of truth — the static `schemas/workflow-request.schema.json` file has been deleted. Never add client-only required fields that the server does not require. See J36 for the fetch pattern. See also J34 (TypeScript interface drift).

## J31: Workflow JSON actions need the right transport and imports

**Why:** JSON workflow actions post directly to `/plan` and `/execute`. If the workspace keeps using the generic form transport, the action payload shape and response handling drift from the server contract. Missing imports for action helpers fail only at click time, which is easy to miss in static inspection.

**What:** Use the JSON transport helper for workflow actions, import the action handlers at the component boundary, and cover the click path in unit tests so missing imports are caught before Playwright waits on a response.

## J32: ajv v8 + JSON Schema draft 2020-12

**Why:** The default `ajv` package export supports Draft 07 and 2019-09 only. Passing a schema that declares `"$schema": "https://json-schema.org/draft/2020-12/schema"` to the default `Ajv` constructor raises `Error: no schema with key or ref "https://json-schema.org/draft/2020-12/schema"` because the draft-2020-12 implementation lives in a separate entry point bundled inside the same package.

**What:** Import from the sibling entry point — no extra install needed:

```ts
import Ajv2020 from 'ajv/dist/2020';

const _ajv = new Ajv2020();
const _validate = _ajv.compile(schema);
```

**Compile-time failure behaviour:** `_ajv.compile(schema)` is called at module load time (top-level constant), so a malformed schema fails the import, not runtime. This is desirable — the error surfaces at bundle load rather than at first user interaction — but it can be misread as a bundler error. Check the `ajv` compile stack trace before chasing a bundler misconfiguration.

Reference: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/js/utils/workflowJsonSchema.ts`.

## J33: ARIA announcement pattern for async forms (E2a unified-result variant)

The Workflow JSON Editor uses a layered ARIA attribute set. Each attribute does exactly one job; they do not stack. Client-side Ajv errors and server-side action results now flow through a **single** result pane tagged with a source badge — the separate `role="alert"` inline-error element is gone.

**Why:** Without explicit ARIA roles, screen readers either announce nothing (silent submit) or announce everything twice (double-announce on `aria-live` + focus shift). Consolidating Ajv and server feedback into one `role="status"` region keeps announcements single-channel while preserving live red-border feedback on the textarea via the `is-invalid` class.

**What:**

```tsx
<div aria-busy={isBusy} className="workflow-json-workspace">
	<div role="toolbar" aria-label="Workflow JSON">
		<button disabled={!canPlan || isBusy}>Plan</button>
		<button disabled={!canExecute || isBusy}>Execute</button>
	</div>

	{/* Decorative progress bar — aria-hidden prevents double-announce */}
	<progress aria-hidden="true" />

	<section className="sheet">
		<textarea
			aria-describedby={
				result?.action === 'ajv' ? 'workflow-json-result-panel' : undefined
			}
			aria-invalid={
				liveValidity === 'invalid' ||
				(result !== null && result.action === 'ajv')
			}
			className={liveValidity === 'invalid' ? 'is-invalid' : ''}
			readOnly={isBusy}
		/>

		{/* Unified async result — role="status" covers Ajv and server responses */}
		{result ? (
			<section id="workflow-json-result-panel" role="status" aria-live="polite">
				<span className={`workflow-json-source--${result.action}`}>
					{result.actionLabel}
				</span>
				…
			</section>
		) : (
			<div>Run Execute or Plan to see results.</div>
		)}
	</section>
</div>
```

- `role="status" aria-live="polite"` on the result region — announces both Ajv and backend responses when they arrive without interrupting current speech.
- `aria-describedby` pointing at the result pane's `id` **only** when the latest result is Ajv-sourced — screen readers associate the textarea with the Ajv error message, but the link is dropped once editing resumes or the next non-Ajv result arrives.
- `aria-invalid` is true for both live-syntax invalid JSON *and* a current Ajv-sourced result. The Bootstrap `is-invalid` visual class is intentionally scoped to live invalid syntax only — Ajv errors show in the result pane, not as a field-level red border.
- `aria-hidden="true"` on the progress bar — the bar is decorative; hiding it prevents the screen reader from announcing "25% complete" over the live region.
- `aria-busy={isBusy}` on the **root `<div>`** only. Do NOT duplicate it on the inner `<section>` — two carriers cause assistive tech to announce the busy state twice.
- `role="toolbar"` with `aria-label` on the toolbar `<div>` — the root changed from `<section>` to a generic `<div>`, so the toolbar needs an explicit landmark for keyboard users navigating by role.
- `readOnly={isBusy}` on the textarea and `disabled={isBusy}` on action buttons — prevent edits and double-submits during in-flight requests.

Reference: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/js/components/WorkflowJsonEditor.tsx`.

## J34: Schema vs TypeScript type drift

**Why:** The runtime JSON Schema served by `WorkflowResource._schemaDocument()` and the `WorkflowRequestPayload` TypeScript interface are two sources of truth. They can drift silently: a field marked `required` in the TS interface but `optional` in the schema causes schema-valid payloads to fail TS type guards, and vice versa. `const`, `enum`, `anyOf`, and `minItems` constraints live only in the schema and have no direct TS counterpart, making parity easy to miss.

**What:** Until a codegen pipeline (`json-schema-to-typescript` or equivalent) is wired, keep the two in sync manually:

- When modifying `WorkflowResource._schemaDocument()`, re-check the corresponding TS interface fields for required-vs-optional parity.
- When modifying the TS interface (`types/index.ts`), re-check the schema `required` arrays for consistency.

A future ADR should evaluate codegen to eliminate the manual sync requirement.

Reference: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/workflow/jaxrs/WorkflowResource.java#_schemaDocument` and `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/js/types/index.ts`.

## J35: Debounced live-validation (single-state, unified-result variant)

**Why:** A single validation state forces a choice between showing a red border on the empty initial render (bad UX — the user hasn't done anything wrong yet) or suppressing the red border until a button click (bad UX — the user gets no inline feedback while typing). Splitting live feedback from action-triggered feedback into separate **surfaces** (textarea class vs. result pane) lets one state field drive both, without the awkward two-state synchronization the earlier design required.

**What:**

- `liveValidity: 'empty' | 'invalid' | 'ok'` — updated on every `onChange` after a 300 ms debounce. Drives the `.is-invalid` CSS class only. `'empty'` is the pre-input state and never flips the class.
- `result: WorkflowJsonWorkspaceResult | null` — set when the user clicks an action button (Execute / Plan) OR when Ajv rejects the payload on action press. The Ajv path sets `result.action === 'ajv'`; the server paths set `result.action === 'plan' | 'execute' | 'load'`. Result is cleared only on first `onChange` keystroke **when the current result is Ajv-sourced** — non-Ajv results persist until the next action completes.

Debounce cleanup pattern:

```ts
const _debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

const handleChange = (value: string) => {
	if (_debounceRef.current) clearTimeout(_debounceRef.current);
	if (result !== null && result.action === 'ajv') {
		setResult(null);                         // clear Ajv result only
	}
	_debounceRef.current = setTimeout(() => {
		setLiveValidity(value === '' ? 'empty' : validate(value) ? 'ok' : 'invalid');
	}, 300);
};

useEffect(() => () => {
	if (_debounceRef.current) clearTimeout(_debounceRef.current);
}, []);
```

Reference: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/js/components/WorkflowJsonEditor.tsx`.

## J36: `/o/<app>/` JAX-RS fetch must send `credentials` AND `x-csrf-token`

**Why:** Liferay DXP 2026's PortalRealm auth verifier rejects a `/o/<app>/...` request that carries only `JSESSIONID` with HTTP 401 and `WWW-Authenticate: Basic realm="PortalRealm"`. The failure mode is silent from the user's perspective: the on-mount fetch rejects, `schemaStatus` stays on `'error'`, every downstream button stays `disabled`, and a Playwright `waitFor(:not([disabled]))` times out without any UI-visible error. A fetch that is missing this pair looks identical to an endpoint that does not exist.

**What:** Every `fetch(...)` targeting `/o/<app>/...` MUST set both:

```ts
fetch('/o/ldf-workflow/schema', {
	credentials: 'include',                        // sends JSESSIONID
	headers: {'x-csrf-token': Liferay.authToken},  // proves authenticated UI origin
});
```

Equivalent alternative to the header: append `?p_auth=${Liferay.authToken}` to the URL. Either form is enough, but one is mandatory — Liferay also rejects `credentials: 'include'` alone.

Pair every new JAX-RS `fetch` with a Vitest assertion that locks both options (`expect(init.credentials).toBe('include')`, `expect(init.headers['x-csrf-token']).toBe(Liferay.authToken)`) so a future author who copies the call site loses the auth pair. `test/js/utils/workflowJsonSchema.test.ts` is the reference.

Reference: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/js/utils/api.ts` (`postJsonResource`) and `utils/workflowJsonSchema.ts`.
