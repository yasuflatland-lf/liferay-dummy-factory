# Workflow API reference

The JAX-RS application at `/o/ldf-workflow` that runs multi-step workflows. This file is the source of truth for the request/response contract and per-operation behaviour. A gentler introduction for users is [`guides/workflows.md`](../guides/workflows.md); the editor UI is described in [`architecture/frontend.md`](../architecture/frontend.md#workflow-json-tab).

## Endpoints

| Method and path | Purpose |
|---|---|
| `GET /o/ldf-workflow/functions` | Registered operations with parameter metadata (type, required, description, default) |
| `GET /o/ldf-workflow/schema` | JSON Schema of the request; includes the current `operation` enum and the `from` syntax |
| `POST /o/ldf-workflow/plan` | Validate a workflow without running it |
| `POST /o/ldf-workflow/execute` | Validate, then run the steps |
| `POST /o/ldf-workflow/operations/{operation}` | Run one operation with literal parameters — see [per-operation tools](#per-operation-tools) |
| `GET /o/ldf-workflow/openapi.json` | OpenAPI 3.1.0 document, JSON only — see [MCP tool set](#mcp-tool-set) |

Registration uses the OSGi JAX-RS whiteboard: `osgi.jaxrs.application.base=/o/ldf-workflow`, `osgi.jaxrs.name=ldf-workflow`, and `osgi.jaxrs.application.select=(osgi.jaxrs.name=ldf-workflow)` on the resource.

### Authentication

Browser callers must send the session cookie **and** a CSRF token ([why](../architecture/frontend.md#server-communication)). Scripts authenticate with Basic Auth.

`execute` and `operations/{operation}` reject a Guest (or unresolvable) user with HTTP 401 and a `Basic realm="PortalRealm"` challenge. A signed-in user must be a company administrator; otherwise the response is HTTP 403 with a `FORBIDDEN` error. `company.create` additionally requires an omniadmin, including when it appears in a later workflow step. These checks run before validation or execution. Steps always run as the signed-in user and that user's company. Step parameters cannot override `userId` or `companyId`: `/execute` ignores these identity parameters, while `operations/{operation}` rejects them as `UNKNOWN_PARAMETER`. The guard lives in `WorkflowResource`, so `functions`, `schema`, `plan` and `openapi.json` stay anonymous. Liferay MCP fetches `openapi.json` without credentials ([Liferay MCP Server](dxp-runtime-config.md#liferay-mcp-server)), and `plan` only validates. MCP invocations are checked as the MCP user because the internal forward carries that user as the `USER_ID` request attribute and `WorkflowResource` builds its own `PermissionChecker` for that user, rather than using MCP's permission checker. The authorization features in `McpToolSetSpec` call `/o/ldf-workflow` directly with Basic Auth (or no credentials for Guest), asserting 401 and non-admin 403 responses and verifying through JSONWS that rejected requests create no roles; they do not test authorization through MCP.

Portlet authorization is described under [Resource commands](../architecture/backend.md#resource-commands).

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

Each step result follows the [batch response contract](../architecture/backend.md#batch-response-contract): `{success, count, requested, skipped, items, error?}`; over HTTP `error` is emitted as `null` on success (see below).

The workflow layer adds its own checks on each step result (`WorkflowStepResult`): `requested`, `count` and `skipped` are non-negative, `count + skipped == requested`, and `success` requires `count == requested`. `error` is required on failure and `null` on success; the workflow HTTP JSON always carries the `error` key. `count` is not required to equal the number of `items`, because `webContent.create` returns one item per site (see the `WebContentCreator` exception in the [batch response contract](../architecture/backend.md#batch-response-contract)).

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
| `webContent.create` | web content articles | takes `groupIds`; a scalar `from` reference is accepted and wrapped in a one-element list; the input `count` is per site; returns one item per target site (`{groupId, siteName, created, failed, error?}`), so `steps.<id>.items[0].groupId` indexes sites, with `requested = count × groupIds.length` and `count` = total articles |
| `vocabulary.create` | vocabularies | needs `groupId > 0` |
| `category.create` | categories | needs `groupId > 0` and a vocabulary |
| `mbCategory.create` | message-board categories | needs `groupId > 0` |
| `mbThread.create` | message-board threads | needs `groupId > 0` |
| `mbReply.create` | replies | requires `count`, `threadId`, `body`; has **no** `baseName` |

- **`count` is capped per step** and larger values are rejected, never truncated; for `webContent.create` the cap applies to `count × groupIds.length` — see the [count cap](../architecture/backend.md#parameters-batchspec-and-batchspec).
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

## MCP tool set

`GET /openapi.json` turns this application into a Liferay MCP Server tool set (design: [ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md); container setup: [Liferay MCP Server](dxp-runtime-config.md#liferay-mcp-server)).

- **Publication**: `WorkflowResource` carries `openapi.resource=true` and `openapi.resource.path=/ldf-workflow` and exposes `getOpenAPI(HttpServletRequest, String, UriInfo)`, which Liferay looks up by reflection. Keep that exact signature. The document is built by `WorkflowOpenAPIDocumentBuilder` from the same request schema `/schema` returns, with `$schema` and `$id` removed, plus the registered function descriptors for the typed per-operation request schemas. There is no `api.version` property and no version path segment.
- **Tool set name**: `ldf-workflow`.
- **Tools** (`operationId`s): `getWorkflowFunctions` (`GET /functions`), `getWorkflowSchema` (`GET /schema`), `planWorkflow` (`POST /plan`), `executeWorkflow` (`POST /execute`), plus one tool per operation ([per-operation tools](#per-operation-tools)). **The operationIds are public API: do not rename them.** A rename breaks every MCP profile that pinned the tool. `WorkflowOpenAPIDocumentBuilderTest.operationIdsAreStable` locks the four coarse names; `McpToolSetSpec` locks all 18 names.
- **Invocation through the `default` profile** uses the meta tool `postToolSetToolSetNameToolInvoke`. A tool with a JSON request body takes the workflow request nested twice under `body`: `{"toolSetName": "ldf-workflow", "toolName": "planWorkflow", "body": {"body": <workflow request>}}`. The outer `body` is the meta tool's payload. The inner `body` is how Liferay maps an operation's request body into tool input. Tools without a request body take `"body": {}`.
- **Error mapping**: Liferay reports `isError=true` only for HTTP status >= 300. `/plan` and authorized `/execute` requests return 200 even on validation errors or a failed step, so an MCP caller must read `errors` and `execution.status` in the body. Per-operation tools do surface failures as `isError=true` because they return HTTP 400, 404 or 422. Authentication and authorization failures also surface as `isError=true` ([Authentication](#authentication)).
- **Stale definitions after a redeploy (measured on the pinned DXP image)**: after changing a `summary` in the builder and redeploying the JAR into a running container, `/o/ldf-workflow/openapi.json` served the new text right away, but `getToolSetToolSetNameToolSummariesPage` and `getToolSetToolSetNameTool` kept the old text until the container was restarted. Liferay caches each tool set's OpenAPI document per company in memory. **Workaround: restart the container** (`docker restart <container>`; verified) after a redeploy that changes the OpenAPI document.
- **Regression guard**: `McpToolSetSpec`.

### Per-operation tools

Each per-operation tool calls `POST /operations/<operation>` with a plain parameter object (for example `{"count": 3, "baseName": "mcpuser"}`). It accepts **literal values only, no `from`**. It runs one step through the workflow engine and returns the step result directly: `{stepId, operation, status, result: {success, requested, count, skipped, items, error}, error}`. Through the MCP meta tool, nest the parameter object under `body.body` as for the coarse POST tools.

This table is the single source of truth for per-operation tool names. `WorkflowOpenAPIDocumentBuilder`'s fixed operationId table must match it, and `WorkflowOperationIdCoverageTest` checks coverage and uniqueness.

| Workflow operation | operationId |
|---|---|
| `blogs.create` | `createBlogsEntries` |
| `category.create` | `createCategories` |
| `company.create` | `createCompanies` |
| `document.create` | `createDocuments` |
| `layout.create` | `createLayouts` |
| `mbCategory.create` | `createMBCategories` |
| `mbReply.create` | `createMBReplies` |
| `mbThread.create` | `createMBThreads` |
| `organization.create` | `createOrganizations` |
| `role.create` | `createRoles` |
| `site.create` | `createSites` |
| `user.create` | `createUsers` |
| `vocabulary.create` | `createVocabularies` |
| `webContent.create` | `createWebContents` |

HTTP status contract:

| Status | Meaning | Body |
|---|---|---|
| 200 | Step succeeded | Step result with `status: SUCCEEDED` |
| 400 | Request or plan validation failed (for example a missing required parameter) | `errors` list |
| 400 | Body contains keys absent from the operation descriptor, including `userId` and `companyId` | `errors` list with `UNKNOWN_PARAMETER`, one per unknown key in sorted order |
| 401 | Guest or unresolvable user; rejected before validation or execution | Authentication challenge |
| 403 | User fails the [authorization rule](#authentication); rejected before validation or execution | `errors` list with `FORBIDDEN` |
| 404 | Operation is unknown or has no mapped tool | `errors` list with `OPERATION_UNKNOWN` |
| 422 | Step failed or adapter threw an exception, including invalid values such as a count out of range | Step result with `status: FAILED` |

The [count cap](../architecture/backend.md#parameters-batchspec-and-batchspec) is checked during adapter execution, so `count: 0` and a count above the cap return 422, while an omitted required `count` returns 400.

### `ldf` profile

The dedicated profile `ldf` at `/o/mcp/ldf` is provisioned by `McpProfileProvisioner`, never by the bundle ([ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md)); run it with the [`setupMcpProfile` task](test-harness.md#mcp-profile-task-setupmcpprofile). It pins every `ldf-workflow` tool, so a tool with a request body takes it nested once under `body` (`{"body": {"count": 1, "baseName": "x"}}`), not twice as with the `default`-profile meta tool.

- **Profile storage on the pinned DXP image (measured)**: `/o/mcp/server-profiles` has only `name`, `description` and a required `tools` text field with one `<toolSetName> <toolName>` line per pinned tool. There is no `profileStatus` picklist, no `instructions` field, and no `/o/mcp/server-profile-tools` object (HTTP 404); those exist only in later liferay-portal `master`. So instead of the create-inactive, upsert-tools, activate sequence planned in ADR-0010, the provisioner upserts the whole profile in one `PUT /o/mcp/server-profiles/by-external-reference-code/LDF_MCP_PROFILE`. Unknown fields in that body are silently ignored.
- **Verification**: Liferay accepts a `tools` line naming a tool that does not exist without error, so the provisioner checks `tools/list` on `/o/mcp/ldf` against the discovered tool set.
- **No restart needed for a profile update**: it is visible on the next `tools/list`. After a redeploy that adds or renames tools, restart the container before re-running `setupMcpProfile`, because the provisioner discovers tools through the cached tool set ([Stale definitions after a redeploy](#mcp-tool-set)).
- **Regression guard**: `McpProfileSpec`.

## Limitations

- Sequential execution only; `FAIL_FAST` only.
- The JSON Schema describes the generic step shape. Per-operation required parameters are exposed through `/functions` and enforced at validation time, not encoded as schema branches. Typed per-operation request schemas exist only in `openapi.json`, for the [per-operation tools](#per-operation-tools).
