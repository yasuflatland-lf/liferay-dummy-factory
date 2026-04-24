# Workflow API

This document describes the workflow JAX-RS API mounted at `/o/ldf-workflow`.

## Endpoints

- `POST /o/ldf-workflow/plan`
- `POST /o/ldf-workflow/execute`
- `GET /o/ldf-workflow/schema`
- `GET /o/ldf-workflow/functions`

## Contract Shape

- `plan` and `execute` reuse the existing workflow DTOs in `com.liferay.support.tools.workflow.dto`.
- Requests use `schemaVersion`, optional `workflowId`, optional `input`, and ordered `steps`.
- Each step executes from top to bottom.
- Step parameters use exactly one of:
  - `value`: literal JSON value
  - `from`: reference to workflow input or an earlier step result
- `value` and `from` are mutually exclusive. A parameter object that carries both is a schema validation error — `plan` and `execute` reject the request before running any step.
- The API is registered with OSGi JAX-RS whiteboard string properties:
  - `osgi.jaxrs.application.base=/o/ldf-workflow`
  - `osgi.jaxrs.name=ldf-workflow`
  - `osgi.jaxrs.application.select=(osgi.jaxrs.name=ldf-workflow)`

## UI Contract Notes

- The workflow JSON workspace now lives in its own tab.
  - `Workflow JSON` is the dedicated authoring surface.
  - Legacy entity forms stay under `Other Entities`.
  - The shell should preserve the old entity default inside `Other Entities` instead of auto-loading workflow content.
- The workflow JSON workspace should not force `workflowId` during client-side preflight.
  - The backend accepts requests without it.
  - Keep the editor aligned with the backend schema instead of adding UI-only required fields.
- Keep workspace metadata separate from the request payload.
  - Sample titles, helper labels, and editor state are client-side concerns.
  - Only the JSON request body should be sent to `/plan` and `/execute`.
- The first render should stay blank unless the user loads a sample explicitly.
  - That keeps the load-sample control meaningful.
  - It also avoids hiding the "what changed" part of the sample loader behind an already-populated editor.
- The editor action strip should stay grouped in one place.
  - `load sample`, `copy`, `schema`, `validate`, `plan`, and `execute` belong together above the editor.
  - The sample picker itself is metadata, not part of the request payload.
- Results should be compact by default.
  - Show a short summary first.
  - Keep the full response behind an explicit details toggle so the editor stays readable.
- `validate` and `plan` are separate UI actions but currently share the same plan resource path.
  - If that backend contract changes, update both the UI wiring and the Playwright assertions together.

## Operational Notes

- `site.create` creates a normal top-level site unless a parent site or other site-specific options are passed explicitly.
- `organization.create` creates an organization and only creates an organization site when the `site` parameter is set to `true`.
- `vocabulary.create` and `category.create` have a narrow startup fallback in the JAX-RS resource: if the OSGi adapter registration is temporarily missing, the resource registers those two operations directly from the creator services so `/functions` and `/plan` stay usable.
- The fallback is intentionally scoped to taxonomy operations only. Other workflow functions still rely on the normal OSGi adapter registration path.
- **Site-scoped operations require `groupId > 0`.** `vocabulary.create`, `category.create`, `layout.create`, `mb-category.create`, `mb-thread.create`, and `wcm.create` all validate `groupId > 0` and fail immediately on zero. The correct template pattern is to chain a `site.create` step first and reference its result: `{"name": "groupId", "from": "steps.createSite.items[0].groupId"}`. Passing `groupId: 0` as a "use default site" hint is not supported.
- **`webContent.create` `groupIds` accepts a scalar via internal coercion.** The parameter is declared as a long array, but `requirePositiveLongArray` delegates to `_asLongList`, which wraps a scalar value in a single-element list. A `from:` reference that resolves to a single `groupId` (e.g. `"from": "steps.createSite.items[0].groupId"`) is therefore accepted by a parameter named `groupIds` (plural). No array literal is needed in the template.
- **`mbReply.create` does not accept `baseName`.** The reply entity is posted under an existing thread and there is no per-entity name to enumerate. The required parameters are `count`, `threadId`, and `body`. Do not add a `baseName` field; the request record will fail schema validation.
- **Optional workflow parameters use `WorkflowParameterValues.optionalBoolean` / `optionalString` (and the `optionalLong` family).** These helpers return a caller-supplied default when the parameter is absent from the request, which is how adapters let templates omit flags like `fakerEnable` or `locale` and still get deterministic behaviour. Required parameters use the non-optional `boolean(...)`, `string(...)`, `long(...)` variants, which throw on absence. Do not reinvent this with `values.has(...)` + ternary — the `optional*` helpers are the API contract.
- **`document.create` with empty or omitted `uploadedFiles` auto-generates placeholder content.** When `DocumentCreator._loadTempFiles()` returns an empty list, the Creator falls through to a placeholder branch that generates `"Test document: <title>".getBytes(UTF_8)` for each entity. This enables end-to-end workflow coverage of the document operation without requiring the file-upload UI flow.

## RC ↔ Workflow Adapter Field Parity

The `toJson` lambda of each `*ResourceCommand` and the `itemMapper` of its corresponding `*WorkflowOperationAdapter` must expose **identical fields in the same order**. Both paths create the same entities; workflow `${ref}` references (e.g. `steps.createCategory.items[0].groupId`) must resolve identically whether the step ran via the portlet RC or via the workflow engine.

**Canonical field sets by entity:**

| Entity | Fields |
|---|---|
| Category | `categoryId`, `groupId`, `vocabularyId`, `name` |
| Vocabulary | `vocabularyId`, `groupId`, `name` |
| MBCategory | `categoryId`, `groupId`, `name` |
| MBThread | `categoryId`, `groupId`, `messageId`, `subject`, `threadId` |
| Organization | `name`, `organizationId` |
| Role | `name`, `roleId`, `type` |
| Blogs | `entryId`, `title` |
| MBReply | `body`, `messageId`, `subject` |
| Layout | `friendlyURL`, `layoutId`, `name`, `plid` |

When adding a field to one layer, add it to the other in the same PR. The Creator returns the Liferay model object directly, so any getter on that model can be exposed without a UseCase intermediary.

## Reference Syntax

- `input.<property>[.<nestedProperty>|[index]...]`
- `steps.<stepId>.<property>[.<nestedProperty>|[index]...]`

Examples:
- `input.pageTitle`
- `steps.createSite.items[0].groupId`
- `steps.createSite.data.slug`

## Execution Model

- `plan` validates request shape plus workflow semantics:
  - unknown operations
  - duplicate step ids
  - duplicate parameter names within a step
  - invalid `from` expressions
  - references to later or missing steps
  - missing required parameters defined by each registered workflow function
- `execute` runs the same validation first.
- If validation fails, `execute` returns `errors` and leaves `execution` as `null`.
- If validation succeeds, steps run sequentially and each successful step result is available to later `from` references.
- Only `FAIL_FAST` is supported right now, so execution stops at the first failing step.

## Discovery Endpoints

- `GET /schema`
  - returns the generic JSON Schema for workflow requests
  - includes the currently available `operation` enum values
  - documents `from` reference syntax
- `GET /functions`
  - returns the currently registered workflow functions
  - includes per-function parameter metadata, required flags, descriptions, and defaults when available

## Execute Response

- `execution`
  - `null` when request validation fails
  - otherwise a `WorkflowExecutionResult`
- `errors`
  - empty on successful validation
  - populated with structured validation errors when the request is invalid

## Example

```json
{
  "schemaVersion": "1.0",
  "workflowId": "sample-site-pipeline",
  "input": {
    "pageTitle": "Welcome"
  },
  "steps": [
    {
      "id": "createSite",
      "operation": "site.create",
      "idempotencyKey": "site-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "Demo Site"}
      ]
    },
    {
      "id": "createLayout",
      "operation": "layout.create",
      "idempotencyKey": "layout-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "Home"},
        {"name": "groupId", "from": "steps.createSite.items[0].groupId"},
        {"name": "type", "value": "portlet"}
      ]
    }
  ]
}
```

## Bundled sample templates

The portlet bundles the following sample workflow templates, loadable from the UI sample dropdown or from the test classpath at `integration-test/src/test/resources/workflow-samples/`.

| sample id | operations exercised | purpose |
|---|---|---|
| `site-and-page` | `site.create`, `layout.create` | Minimal two-step dependency chain — page inherits the site's groupId. |
| `company-user-organization` | `company.create`, `user.create`, `organization.create` | Flat sequence of independent top-level entity creations. |
| `vocabulary-and-category` | `site.create`, `vocabulary.create`, `category.create` | Taxonomy chain where the category references both the site groupId and the vocabulary id. |
| `role` | `role.create` | Single standalone step — site-independent global entity. |
| `documents` | `site.create`, `document.create` | Exercises `DocumentCreator`'s placeholder-.txt path (no `uploadedFiles`) and multi-item `baseName` expansion (`count: 2`). |
| `blogs-and-web-content` | `site.create`, `blogs.create`, `webContent.create` | Two entities consuming one parent step's `groupId` in parallel. `webContent` uses scalar-coerced `groupIds`. |
| `message-boards` | `site.create`, `mbCategory.create`, `mbThread.create`, `mbReply.create` | Longest dependency chain — 4 steps with `from:` references threading through category → thread → reply. |

The Spock integration spec `WorkflowSampleTemplateSpec` loads these fixtures from the classpath; the TypeScript `_sampleDefinitions` in `workflowJsonWorkspace.ts` carries structurally equivalent copies (deep-equal, key order is free) for the UI. A Vitest parity test (`workflowJsonWorkspace.parity.test.ts`) enforces deep-equal between the two.

## Current Limitations

- Only sequential execution is supported.
- Only `FAIL_FAST` is supported for `onError.policy`.
- The JSON Schema is generic by step shape; per-operation required parameters are exposed through `/functions` and runtime validation rather than embedded as operation-specific JSON Schema branches.
- The UI workspace is intentionally thinner than the backend schema:
  - it preflights shape and required step structure
  - it does not invent extra required fields that the server does not require
