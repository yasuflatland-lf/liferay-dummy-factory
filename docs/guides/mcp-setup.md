# Use Dummy Factory from Claude Code (MCP)

Liferay's built-in MCP Server exposes this bundle's `/o/ldf-workflow` tools ([ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md)). Nothing extra is installed: there is no separate MCP server process.

This setup is for local development only: it creates dummy data with Basic Auth against the development container.

## Prerequisites

- A DXP that ships the Liferay MCP Server ([ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md) names the first release). The development container uses the version pinned in `gradle.properties`.
- The Dummy Factory bundle deployed and active.
- The MCP Server enabled ([how](../reference/dxp-runtime-config.md#liferay-mcp-server)). The development container already enables it.
- A DXP activation key ([license](../reference/test-harness.md#license)).
- The Claude Code CLI.

## 1. Start Liferay

Run from the repository root. The task builds the bundle into the image:

```bash
./gradlew startDockerContainer
```

Startup takes several minutes. Wait until `/c/portal/login` answers:

```bash
until curl -sf -o /dev/null http://localhost:8080/c/portal/login; do sleep 10; done
```

Task details: [Run DXP locally](development.md#run-dxp-locally).

## 2. Create the `ldf` profile

```bash
./gradlew :integration-test:setupMcpProfile
```

Connection properties and which password to pass: [`setupMcpProfile` task](../reference/test-harness.md#mcp-profile-task-setupmcpprofile). The last line the task prints, before Gradle's `BUILD SUCCESSFUL`, is:

```text
MCP profile 'ldf' ready at http://localhost:8080/o/mcp/ldf
```

The lines before it list the pinned tool names, one per line. What the profile contains: [`ldf` profile](../reference/workflow-api.md#ldf-profile).

## 3. Register the server in Claude Code

Encode the same user and password you passed to `setupMcpProfile`:

```bash
printf '%s' 'test@liferay.com:test' | base64
```

Use that value in the header:

```bash
claude mcp add --transport http ldf http://localhost:8080/o/mcp/ldf \
  --header "Authorization: Basic <base64 of user:password>"
```

Run `/mcp` inside Claude Code to confirm that the `ldf` server is connected and lists its tools.

## 4. Try it

| Prompt | Expected tool(s) |
|---|---|
| "Create 5 users with base name demo-user" | `createUsers` |
| "Create a site called demo-site and add 1 web content to it" | `planWorkflow` then `executeWorkflow`; the web content step needs the new site's `groupId`. |
| "What can you create?" | `getWorkflowFunctions` |

Known issue: a web content step with `count` greater than the number of target sites currently fails with `count must match items size` (HTTP 422). Keep it at one web content per site until [#163](https://github.com/yasuflatland-lf/liferay-dummy-factory/issues/163) is fixed.

Claude Code chooses the tools itself, so a run can differ: it may chain `createSites` and `createWebContents` instead of one workflow, or answer the last prompt from the tool list without calling `getWorkflowFunctions`.

## Without the `ldf` profile

The `default` profile (`/o/mcp`) still reaches every tool through Liferay's meta tools: `getToolSetsPage` → `getToolSetToolSetNameToolSummariesPage` → `postToolSetToolSetNameToolInvoke`. It needs more round trips, and the LLM gets no typed schemas up front. The request body nesting differs: [MCP tool set](../reference/workflow-api.md#mcp-tool-set).

## Limits

- `count` has a [cap per request](../architecture/backend.md#parameters-batchspec-and-batchspec) (per workflow step).
- `executeWorkflow` does not mark a failed step as `isError`; read `execution.status` and `errors` in the result ([error mapping](../reference/workflow-api.md#mcp-tool-set)).

If something does not work, see [Troubleshooting → MCP](troubleshooting.md#mcp).
