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
- **The OpenAPI document is served as JSON only**, at `GET /openapi.json`. Liferay reads tool sets through `OpenAPIDocument.Type.JSON` only, so a YAML variant would have no consumer. The resource keeps the `getOpenAPI(HttpServletRequest, String, UriInfo)` signature that Liferay invokes by reflection.

### Batch size cap (all entry points)

An LLM can emit `count: 100000` as easily as `count: 10`. The cap is enforced at the input boundary and applies to **every** creation path, not only MCP. That covers the portlet UI, `/execute`, and `/operations/*`. The UI and MCP must not disagree about what is a valid request.

- `MAX_BATCH_COUNT = 1000` per request (per workflow step). A request above the cap is **rejected** with an error that states the limit. It is never truncated (input boundary policy).
- The check lives where `count` is already validated: the `BatchSpec` compact constructor, which every `*BatchSpec` composes and every workflow adapter constructs, and `ResourceCommandUtil.validateCount` for `CompanyCreator`, which takes a raw `count`. No per-entry-point copies.
- The cap is per step. A multi-step `executeWorkflow` can create more than 1000 entities in total. That is accepted: each step is still bounded, and the plan is visible via `planWorkflow` before execution.
- The frontend count inputs may mirror the cap as a `max` attribute for UX, but the server-side check is the contract.

### Dedicated MCP profile provisioning: repository script, not the bundle

The `default` profile reaches our tools only through the meta tools: list, get schema, generic invoke. The generic invoke tool takes an opaque `body`, so the LLM gets no typed schema up front. First-class, typed `createUsers` / `createSites` tools need a dedicated profile (e.g. `/o/mcp/ldf`) with each tool pinned.

The profile is created by a **script in this repository** that calls Liferay's public REST endpoints, `/o/mcp/server-profiles` and `/o/mcp/server-profile-tools`. These are the same endpoints the MCP Server admin UI uses. The script:

- derives the tool list from `GET /o/ldf-workflow/functions` plus the four coarse tools, so new operations need no script change;
- is idempotent. Liferay rejects an active profile with zero tools and duplicate tool rows, so the script creates the profile inactive, upserts the tools, then activates it;
- is the same code the integration tests use as their fixture, and is exposed to developers as a Gradle task.

Alternatives rejected:

- **Auto-provision from the bundle on activation.** Profiles are stored as system Objects whose internal field names (e.g. `r_mcpServerProfileToTools_l_mcpServerProfileERC`) belong to a beta feature. That data model has already changed once (LPD-101183 removed the `tools` field with an upgrade process). Writing to it from production bundle code means a DXP update can break bundle activation. It would also need feature-flag guards. And it writes into an admin-owned decision (which tools a profile exposes) on the admin's behalf.
- **Manual setup documented in a guide.** The dev and test container is recreated from the image on every cycle (`.claude/rules/testing.md`, Container setup). The roughly 18 tools would have to be re-pinned by hand each time. The integration tests need the automation anyway.

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
- **Stale tool definitions after a redeploy.** Liferay caches each tool set's OpenAPI JSON per company in a static map (`ToolSetUtil._openAPIJSONObjects`). It clears that map only when an Object definition, field, action, or relationship changes. Each profile's MCP servlet also snapshots tool schemas until that profile changes. Redeploying the bundle with a new or changed operation may therefore leave MCP clients seeing the old definitions. This directly affects the local edit → deploy → try loop. The Phase 0 spike must measure it. The fallback is to restart the container after a redeploy that changes the operation set; re-running the profile script refreshes only the servlet snapshot, not the OpenAPI cache.
- **The profile REST endpoints belong to a beta feature too.** The provisioning script depends on `/o/mcp/server-profiles` and `/o/mcp/server-profile-tools`. If they change, the script and the integration tests break, but the bundle is unaffected. That is the reason this dependency lives in the repository and not in the bundle.
- **The batch cap is a behavior change for the portlet UI.** Requests above 1000 that used to be accepted are now rejected. This is intentional (one rule for every entry point) and must be noted in the release notes.
- **Dev environment needs the feature flag.** `configs/common/portal-ext.properties` gains `feature.flag.LPD-63311=true`, and the MCP Server must be switched on in Instance Settings (or via configuration) for integration tests.
