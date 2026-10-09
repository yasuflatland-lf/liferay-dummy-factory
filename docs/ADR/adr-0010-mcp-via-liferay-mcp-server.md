# ADR-0010: Expose data creation to MCP clients through Liferay's built-in MCP Server

## Status

Proposed — becomes Accepted once the Phase 0 spike (below) confirms the discovery behavior on `liferay/dxp:2026.q3.6`.

## Date

2026-10-09

## Context

We want an AI agent (primarily Claude Code on a developer's machine, against a local DXP container) to create dummy data through MCP: "create 3 sites, each with 2 vocabularies and 20 web contents, plus 50 users in one organization".

The bundle already has everything except the MCP entry point. The workflow JAX-RS application at `/o/ldf-workflow` (contract in `docs/details/workflow-api.md`) exposes:

- `GET /functions` — every registered operation with typed parameters, built from `WorkflowFunctionDescriptor`
- `GET /schema` — the workflow request grammar
- `POST /plan` — validation / dry run
- `POST /execute` — runs an ordered list of steps; later steps reference earlier results via `from: steps.<id>.*`

The open question was where the MCP protocol endpoint lives. Three options were considered:

- **A. In-bundle MCP endpoint** — implement JSON-RPC (`initialize`, `tools/list`, `tools/call`) ourselves in a new JAX-RS application.
- **B. External Node/TypeScript stdio server** — a separate package that calls `/o/ldf-workflow` over HTTP.
- **C. Liferay's built-in MCP Server** — reuse the DXP feature, if it offers an extension point.

### What Liferay's MCP Server does

The following was read from `liferay/liferay-portal` `master` (`modules/apps/mcp/mcp-server-rest-impl`, `modules/apps/portal-vulcan/portal-vulcan-impl/.../HeadlessApplicationProviderImpl.java`):

- It is a DXP 2026.Q3+ feature behind the `LPD-63311` feature flag, then enabled per instance in Instance Settings. The endpoint is `/o/mcp` (the `default` profile) and `/o/mcp/<profile>` for other profiles. `MCPServerServlet` uses the official MCP Java SDK (`HttpServletStatelessServerTransport`).
- **Tool sets are discovered, not registered.** `HeadlessApplicationProvider` enumerates every application in the JAX-RS runtime (`JaxrsServiceRuntime.getRuntimeDTO()`), not only the REST Builder ones. An application becomes a tool set when it has a resource method whose path contains `/openapi`, and a service registered with `openapi.resource=true` and `openapi.resource.path=<application base>` that exposes a public `getOpenAPI(HttpServletRequest, String, UriInfo)` method. Liferay calls that method by reflection. An `api.version` service property must equal the version path segment, and both are absent for an unversioned path.
- The tool set name is the base path plus the optional version, with `/` turned into `-`. `/ldf-workflow` with an unversioned `/openapi.json` becomes `ldf-workflow`.
- **Tools come from the OpenAPI document.** `operationId` is the tool name. `summary` + `description` is the tool description. Path, query, and body parameters become `inputSchema`. A JSON request body must be nested under a `body` property of the tool input.
- **Invocation is an internal forward.** `ToolSetUtil.invokeTool` → `VulcanRequestForwarder` → `<basePath><operation path>`, as the authenticated MCP user.
- **Error mapping.** An HTTP status `< 300` becomes `CallToolResult.isError=false`. Any other status becomes `isError=true` with the body as text.
- **Profiles choose the exposed tools.** The seeded `default` profile carries only four meta tools: list tool sets, list tool summaries, get a tool's schema, and invoke a tool by name. So any discovered tool set is reachable through `default` with no admin setup. Tools pinned to a profile appear as first-class MCP tools on `/o/mcp/<profile>`.

## Decision

**Adopt option C.** Do not implement any MCP protocol code. Instead, make `/o/ldf-workflow` a Liferay MCP tool set by publishing an OpenAPI document for it.

### Tool surface (hybrid)

| `operationId` | Path | Role |
|---|---|---|
| `getWorkflowFunctions` | `GET /functions` | Coarse: discover operations |
| `getWorkflowSchema` | `GET /schema` | Coarse: workflow grammar |
| `planWorkflow` | `POST /plan` | Coarse: dry run / validation |
| `executeWorkflow` | `POST /execute` | Coarse: multi-step creation with `from:` chaining |
| one per operation, e.g. `createUsers`, `createSites` | `POST /operations/<operation>` | Fine-grained: single-step creation |

- **The OpenAPI document is generated at request time from the `WorkflowFunctionRegistry` / `WorkflowFunctionDescriptor`.** Nothing is hand-written. A new workflow adapter therefore becomes a new MCP tool with no extra code, and parameter names, types, and descriptions keep a single source of truth.
- **Fine-grained tools share one JAX-RS method** (`@POST @Path("operations/{operation}")`). The OpenAPI document lists one concrete path per operation, so each gets its own `operationId` and request-body schema. The method runs the call as a one-step workflow through the existing `WorkflowEngine`. Creation logic is not duplicated.
- **Failure must be a non-2xx status on the fine-grained endpoint.** This makes the MCP layer report `isError=true`. The response body stays the normal step result (`{success, count, requested, skipped, error, items}` per ADR-0009). The existing `/plan` and `/execute` status codes are unchanged, because the portlet UI and `WorkflowHttpE2ESpec` depend on them. Their bodies already carry validation errors and step status.
- **Lookup tools (sites, vocabularies, …) are not built by us.** Liferay's own headless applications (e.g. headless-admin-site, headless-admin-taxonomy) are discovered as tool sets by the same mechanism. A dedicated lookup operation is added only if a concrete gap is found.
- **No cleanup tool.** Out of scope by product decision.

### Out of scope

- An MCP endpoint of our own (options A/B). Revisit only if the Phase 0 spike shows that `2026.q3.6` does not discover custom JAX-RS applications.
- Remote / shared-team usage and OAuth2 flows. The target is local Claude Code with Basic Auth (or a bearer token) against a dev container.

## Consequences

### Benefits

- **No protocol code to maintain.** MCP spec changes, transport, auth (`MCPServerAuthVerifierFilter`), data masks, and profile management are Liferay's responsibility.
- **Single-JAR design is kept.** Installing the bundle is enough. The tools show up in the `default` profile via the meta tools, and can be pinned to a dedicated profile for first-class tool listing.
- **Tool definitions cannot drift from the engine.** They are derived from the same descriptors that `/functions` and `/plan` already use.

### Costs and risks

- **Dependency on a beta feature.** `LPD-63311` is a release-feature flag, and the discovery rules above were read from `master`, not from the `2026.q3.6` tag. The Phase 0 spike must confirm them on the pinned image before any production code is written.
- **The OpenAPI contract becomes public surface.** Renaming an `operationId` breaks any profile that pinned the tool. Operation names are treated as stable once released.
- **`execute` returns 200 even when a step fails.** Through MCP, a failed multi-step run is reported with `isError=false` and the failure is only visible in the body. This is documented in the tool description. Changing it would break the existing UI contract.
- **Dev environment needs the feature flag.** `configs/common/portal-ext.properties` gains `feature.flag.LPD-63311=true`, and the MCP Server must be switched on in Instance Settings (or via configuration) for integration tests.
