# Workflow API reference

The JAX-RS application at `/o/ldf-workflow` that runs multi-step workflows. This file is the source of truth for the request/response contract and per-operation behaviour. A gentler introduction for users is [`guides/workflows.md`](../guides/workflows.md); the editor UI is described in [`architecture/frontend.md`](../architecture/frontend.md#workflow-json-tab).

## Endpoints

| Method and path | Purpose |
|---|---|
| `GET /o/ldf-workflow/functions` | Registered operations with parameter metadata (type, required, description, default) |
| `GET /o/ldf-workflow/schema` | JSON Schema of the request; includes the current `operation` enum and the `from` syntax |
| `POST /o/ldf-workflow/plan` | Validate a workflow without running it |
| `POST /o/ldf-workflow/execute` | Validate, then run the steps |

Registration uses the OSGi JAX-RS whiteboard: `osgi.jaxrs.application.base=/o/ldf-workflow`, `osgi.jaxrs.name=ldf-workflow`, and `osgi.jaxrs.application.select=(osgi.jaxrs.name=ldf-workflow)` on the resource.

Browser callers must send the session cookie **and** a CSRF token ([why](../architecture/frontend.md#server-communication)). Scripts authenticate with Basic Auth.

## Request

```json
{
  "schemaVersion": "1.0",
  "workflowId": "sample-site-pipeline",
  "input": {"pageTitle": "Welcome"},
  "steps": [
    {
      "id": "createSite",
      "operation": "site.create",
      "idempotencyKey": "site-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "demo-site"}
      ]
    },
    {
      "id": "createLayout",
      "operation": "layout.create",
      "idempotencyKey": "layout-1",
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "home"},
        {"name": "groupId", "from": "steps.createSite.items[0].groupId"},
        {"name": "type", "value": "portlet"}
      ]
    }
  ]
}
```

- `schemaVersion` and `steps` are required; `workflowId` and `input` are optional.
- Each step requires `id`, `operation` and `idempotencyKey`; `onError` is optional.
- Each parameter has exactly one of `value` (a literal JSON value) or `from` (a reference). Both together is a validation error.

### Reference syntax

```
input.<property>[.<property>|[<index>]]…
steps.<stepId>.<property>[.<property>|[<index>]]…
```

Examples: `input.pageTitle`, `steps.createSite.items[0].groupId`, `steps.createSite.data.slug`. A reference may only point to an earlier step.

## Execution model

- `plan` validates the request shape and the workflow semantics: unknown operations, duplicate step ids, duplicate parameter names in a step, malformed `from` expressions, references to later or missing steps, and missing required parameters (as declared by each operation).
- `execute` runs the same validation first. If it fails, the response carries `errors` and `execution` is `null`.
- Otherwise steps run sequentially; each successful step's result is available to later `from` references.
- `onError.policy` supports only `FAIL_FAST`: execution stops at the first failing step.
- `execute` answers HTTP 200 even when a step fails; the failure is in the body.

### Execute response

| Field | Content |
|---|---|
| `execution` | `null` when validation failed, otherwise a `WorkflowExecutionResult` with one result per step |
| `errors` | empty when validation passed, otherwise structured validation errors |

Each step result follows the [batch response contract](../architecture/backend.md#batch-response-contract): `{success, count, requested, skipped, items, error?}`.

## Operations

`GET /functions` is authoritative for parameters. The table lists what each operation creates and the behaviour that is not visible from parameter metadata.

| Operation | Creates | Notes |
|---|---|---|
| `company.create` | virtual instances | |
| `organization.create` | organizations | Creates an organization site only when `site` is `true` |
| `role.create` | roles | |
| `site.create` | sites | A normal top-level site unless a parent or other site options are passed |
| `user.create` | users | Unless `fakerEnable` is true, `baseName` (lowercased) must match `^[a-z0-9._-]+$` |
| `blogs.create` | blog entries | needs `groupId > 0` |
| `document.create` | documents | needs `groupId > 0`; with no `uploadedFiles` it generates placeholder text files (`"Test document: <title>"`) |
| `layout.create` | pages | needs `groupId > 0` |
| `webContent.create` | web content articles | takes `groupIds`; a scalar `from` reference is accepted and wrapped in a one-element list |
| `vocabulary.create` | vocabularies | needs `groupId > 0` |
| `category.create` | categories | needs `groupId > 0` and a vocabulary |
| `mbCategory.create` | message-board categories | needs `groupId > 0` |
| `mbThread.create` | message-board threads | needs `groupId > 0` |
| `mbReply.create` | replies | requires `count`, `threadId`, `body`; has **no** `baseName` |

- **`count` is capped per step** and larger values are rejected, never truncated — see the [count cap](../architecture/backend.md#parameters-batchspec-and-batchspec).
- **`groupId: 0` is not "the default site".** Site-scoped operations reject it. Chain a `site.create` step and reference `steps.<id>.items[0].groupId`.
- **Optional parameters** are read with the `WorkflowParameterValues.optional*` helpers, which return the documented default when the parameter is absent. Required parameters use the non-optional readers, which fail on absence. Adapters must not re-implement this with `has(...)` checks.
- **Taxonomy startup fallback.** If the OSGi registration of the `vocabulary.create` / `category.create` adapters is momentarily missing, `WorkflowResource` registers those two operations directly from the Creator services so `/functions` and `/plan` keep working. The fallback is limited to these two operations.

## RC ↔ workflow adapter field parity

A resource command's `toJson` lambda and the matching adapter's item mapper expose **the same fields in the same order**, so a `from` reference resolves identically whichever path created the entity. Add a field to both in the same change.

| Entity | Item fields |
|---|---|
| Category | `categoryId`, `groupId`, `vocabularyId`, `name` |
| Vocabulary | `vocabularyId`, `groupId`, `name` |
| MBCategory | `categoryId`, `groupId`, `name` |
| MBThread | `categoryId`, `groupId`, `messageId`, `subject`, `threadId` |
| MBReply | `body`, `messageId`, `subject` |
| Organization | `name`, `organizationId` |
| Role | `name`, `roleId`, `type` |
| Blogs | `entryId`, `title` |
| Layout | `friendlyURL`, `layoutId`, `name`, `plid` |

## Bundled samples

Each sample is one JSON file under `integration-test/src/test/resources/workflow-samples/`. That file is the single source: `WorkflowSampleTemplateSpec` loads it from the classpath, the UI ships a copy in `workflowJsonWorkspace.ts`, and `workflowJsonWorkspace.parity.test.ts` deep-equals the two.

| Sample | Operations | What it demonstrates |
|---|---|---|
| `site-and-page` | `site.create`, `layout.create` | Minimal dependency chain |
| `company-user-organization` | `company.create`, `user.create`, `organization.create` | Independent top-level entities |
| `vocabulary-and-category` | `site.create`, `vocabulary.create`, `category.create` | A step referencing two earlier steps |
| `role` | `role.create` | Single site-independent step |
| `documents` | `site.create`, `document.create` | Placeholder documents and `count: 2` |
| `blogs-and-web-content` | `site.create`, `blogs.create`, `webContent.create` | Two steps consuming one parent; scalar `groupIds` |
| `message-boards` | `site.create`, `mbCategory.create`, `mbThread.create`, `mbReply.create` | Longest chain: category → thread → reply |

Adding a sample: create the fixture first, then the TS copy, then extend the parity test.

## Limitations

- Sequential execution only; `FAIL_FAST` only.
- The JSON Schema describes the generic step shape. Per-operation required parameters are exposed through `/functions` and enforced at validation time, not encoded as schema branches.
