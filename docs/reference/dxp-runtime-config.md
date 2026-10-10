# DXP runtime configuration reference

How the test container is configured so that JSONWS, Basic Auth and the admin login work, and the Liferay behaviours that forced each setting. Everything here lives in `configs/common/` (baked into the image by `dockerDeploy`) or in the test-only components `BasicAuthTestSetup` / `SAPTestSetup`.

If JSONWS calls fail with 401/403 or come back as the Guest user, start at [Basic Auth, step by step](#basic-auth-step-by-step).

## Files in `configs/common/`

| File | Why it exists |
|---|---|
| `portal-ext.properties` | Disables setup wizard / terms / reminder queries / password-change prompts; opens JSONWS; registers Basic Auth on the JSONWS servlet; exempts JSONWS from CSRF; widens the default SAP |
| `portal-liferay-online-config.properties` | **Intentionally empty** — shadows the image's file that blocks JSONWS |
| `osgi/configs/…BasicAuthHeaderAuthVerifierConfiguration-default.config` | Enables Basic Auth on the OSGi verifier pipeline (`/o/*`, `/api/*`) |
| `osgi/configs/…BasicAuthHeaderAuthVerifierConfiguration-ldfworkflow.config` | Basic Auth on `/o/ldf-workflow/*` without `forceBasicAuth`, so Liferay MCP can invoke the workflow tools — see [Liferay MCP Server](#liferay-mcp-server) |
| `osgi/configs/com.liferay.mcp.server.rest.internal.configuration.MCPServerConfiguration.config` | Switches the Liferay MCP Server on — see [Liferay MCP Server](#liferay-mcp-server) |
| `osgi/configs/…SAPConfiguration.config` | SAP defaults for a fresh database |
| `osgi/configs/com.liferay.support.tools.basicauth.BasicAuthTestSetup.config` | Activates `BasicAuthTestSetup` |
| `osgi/configs/com.liferay.support.tools.sap.SAPTestSetup.config` | Activates `SAPTestSetup` |

Per-environment directories (`configs/local`, `dev`, `uat`, `prod`) must **not** contain a `portal-ext.properties` unless it carries real overrides: the env copy replaces the common one wholesale, so an empty file silently disables everything above.

## Configuration precedence in DXP 2026

Lowest to highest:

1. `portal.properties` defaults
2. `portal-ext.properties` `configuration.override.*` entries
3. `osgi/configs/<pid>.config` files
4. `/home/liferay/portal-liferay-online-config.properties` `configuration.override.*` entries (shipped in the image)
5. Runtime `ConfigurationAdmin.update(...)` writes

The image's online-config file is processed after `portal-ext.properties` and re-applied on every restart, so anything it hardens can only be overridden at level 5. To see what it hardens:

```bash
docker exec liferay-dummy-factory-liferay cat /home/liferay/portal-liferay-online-config.properties | grep configuration.override
```

## Basic Auth, step by step

Five independent things must all be true for a Basic Auth JSONWS call from a test to run as the admin:

| # | Requirement | Symptom when missing | Fix |
|---|---|---|---|
| 1 | The image's `portal-liferay-online-config.properties` (which sets `json.servlet.hosts.allowed=N/A`) is shadowed | every JSONWS call 403 from the first test | empty file in `configs/common/` |
| 2 | The JSONWS servlet has its own verifier registration | Basic Auth works on `/o/*` but `/api/jsonws/*` runs as Guest | `jsonws.servlet.auth.verifier.*` in `portal-ext.properties` |
| 3 | JSONWS is exempt from the session CSRF check | POST returns 403 `PrincipalException$MustHaveSessionCSRFToken` | `auth.token.ignore.origins` |
| 4 | The Basic Auth *login support* is enabled | credentials accepted but SAP `SYSTEM_USER_PASSWORD` never applies | `BasicAuthTestSetup` (runtime write) |
| 5 | The password sent is the current admin password | HTTP 401 | `NEW_ADMIN_PASSWORD` after the first-login form |

### 1. Shadow the online-config file

The image bakes `json.servlet.hosts.allowed=N/A` into `/home/liferay/portal-liferay-online-config.properties`, blocking JSONWS from every host. The empty `configs/common/portal-liferay-online-config.properties` replaces it at image build time. Verify:

```bash
docker exec liferay-dummy-factory-liferay cat /home/liferay/portal-liferay-online-config.properties
```

It must be empty. If it still shows `N/A`, rebuild: `./gradlew removeDockerContainer startDockerContainer`.

### 2. Register Basic Auth on the JSONWS servlet filter

`/api/jsonws/*` is served by the JSON Web Service Servlet, whose own `AuthVerifierFilter` reads portal properties with the `jsonws.servlet.` prefix. The OSGi verifier pipeline configured by the `.config` file covers `/o/*` but not this servlet.

```properties
jsonws.servlet.auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes=*
jsonws.servlet.auth.verifier.BasicAuthHeaderAuthVerifier.basic_auth=true
jsonws.servlet.auth.verifier.PortalSessionAuthVerifier.urls.includes=*
```

`urls.includes=*` is enough because the filter is already mounted on `/api/jsonws/*`; the `PortalSessionAuthVerifier` line keeps browser sessions (Playwright, the portlet UI) working. These are portal properties, hence dotted `urls.includes`; the OSGi `.config` uses the OCD name `urlsIncludes`.

The OSGi-side file:

```
enabled=B"true"
urlsIncludes="/api/*,/o/*,/xmlrpc/*"
urlsExcludes="/o/mcp-server/v1.0/openapi.json,/o/headless-admin-site/v1.0/openapi.json,/o/headless-admin-taxonomy/v1.0/openapi.json,/o/headless-admin-user/v1.0/openapi.json,/o/ldf-workflow/*"
forceBasicAuth=B"true"
```

`forceBasicAuth` turns a request with no usable credentials into an HTTP 401 instead of a Guest request — see [When Basic Auth falls through to Guest](#when-basic-auth-falls-through-to-guest) (Liferay translates it to the `basic_auth` filter property, which is why grepping for `basic_auth=true` finds nothing in the OCD). `urlsIncludes` is a **comma-separated String** — see [`.config` syntax](#config-file-syntax). A second instance of the same factory PID, `…BasicAuthHeaderAuthVerifierConfiguration-ldfworkflow.config`, covers only `/o/ldf-workflow/*` with `forceBasicAuth=B"false"`. `urlsExcludes` and the `-ldfworkflow` instance exist only for the Liferay MCP Server — see [Liferay MCP Server](#liferay-mcp-server).

### 3. Exempt JSONWS from the CSRF check

`JSONWebServiceServiceAction` is a `JSONAction`, which requires a session CSRF token; Basic-Auth-only callers have no session.

```properties
auth.token.ignore.origins=com.liferay.portal.remote.json.web.service.web.internal.JSONWebServiceServiceAction
```

This is narrower than `auth.token.check.enabled=false`: other `JSONAction`s still check CSRF. Multiple origins are comma-separated.

### 4. Enable Basic Auth login support at runtime

The online-config file hardens `BasicAuthHeaderSupportConfiguration_enabled=B"false"` at level 4, so neither `portal-ext.properties` nor a `.config` can win. Without it, a successful Basic Auth handshake never sets `passwordBasedAuthentication=true`, the `SYSTEM_USER_PASSWORD` service access policy never applies, and only `SYSTEM_DEFAULT`'s narrow allow-list (Country, Region) is callable.

`BasicAuthTestSetup` writes `enabled=true` through `ConfigurationAdmin` after `portal.initialized`. `BasicAuthHeaderAutoLoginSupport` reads the configuration on every request, so no restart is needed.

### 5. Use the current admin password

DXP 2026 still redirects the default admin to `/c/portal/update_password` on the first browser login, even with `company.security.update.password.required=false` and `passwords.default.policy.change.required=false` (those only remove the legacy `PASSWORDRESET` ticket flow). `BaseLiferaySpec.loginAsAdmin` fills `#password1`/`#password2` with `NEW_ADMIN_PASSWORD` (`Test12345`) when it lands there, and tries `DEFAULT_ADMIN_PASSWORD` (`test`) before `NEW_ADMIN_PASSWORD` so later specs still log in. `LdfResourceClient.login` does the same.

After that, Basic Auth must use the new password; `basicAuthHeader()` is hard-wired to `NEW_ADMIN_PASSWORD`. With login support enabled (step 4), the stale default password gets HTTP 401.

A freshly created regular user gets the same redirect on its first Control Panel request (`/c/portal/update_password?ticketId=…`) because its `User.passwordReset` flag is set, and DXP rejects a new password equal to the current one, so the form only clears when the two differ. `LdfResourceClient` completes the form after login and after opening the portlet. The redirect cannot be cleared from a test over JSONWS: `/user/update-password` is in the default `jsonws.web.service.paths.excludes`. A non-admin needs two grants to reach the Dummy Factory portlet in the Control Panel: `ACCESS_IN_CONTROL_PANEL` on the portlet and `VIEW_CONTROL_PANEL` on the portal resource `90` (`PortletKeys.PORTAL`), both company-scoped. DXP's Roles Admin app grants the same pair when an administrator gives a regular role `ACCESS_IN_CONTROL_PANEL` on a Control Panel portlet (`RolesAdminPortlet._updateViewControlPanelPermission`). `PortletAuthorizationSpec` sets them up through `resourcepermission/add-resource-permission`.

### When Basic Auth falls through to Guest

On OSGi `AuthVerifierFilter` paths such as `/o/*` (liferay-portal source, behaviour measured on the pinned DXP image):

- **Wrong password, login support enabled** (step 4, as in the test container): `BasicAuthHeaderAutoLoginSupport.doLogin` throws `AuthException` when the credentials do not resolve to a user, `BaseAutoLogin` wraps it in `AutoLoginException`, and `BasicAuthHeaderAuthVerifier.verify` answers with a Basic challenge: **HTTP 401, whatever `forceBasicAuth` is set to.** `McpToolSetSpec` locks this for `/o/ldf-workflow`, which runs without `forceBasicAuth` ([Liferay MCP Server](#liferay-mcp-server)).
- **No `Authorization` header**, or **login support disabled** (the image hardening in step 4, where `doLogin` returns `null` without reading the header): the verifier has no credentials. Without `forceBasicAuth` the request continues as Guest — Guest data, or a downstream SAP 403. With `forceBasicAuth=B"true"` the verifier sends a Basic challenge (HTTP 401) instead.

So a silent Guest request comes from missing or ignored credentials, not from a wrong password. The usual test-side trigger is a client that forgets its `Authorization` header, or step 4 not being in effect. Paths moved to the `-ldfworkflow` instance lose the `forceBasicAuth` safety net, which is why `execute` and `operations/*` on `/o/ldf-workflow` reject Guest themselves ([Workflow API → Authentication](workflow-api.md#authentication)).

## Service access policies (SAP)

`SAPServiceVerifyProcess` creates missing `SAPEntry` rows once per company and **never updates existing rows**. Consequently:

- `configuration.override.…SAPConfiguration_systemDefaultSAPEntryServiceSignatures=*` in `portal-ext.properties` only affects a fresh database.
- To widen an existing entry, `SAPTestSetup` calls `SAPEntryLocalService.updateSAPEntry(...)` in-process (setting the allowed signatures to `*`).

`SYSTEM_USER_PASSWORD` applies only when the request's `AuthVerifierResult` has `passwordBasedAuthentication=true`, which is set by `PortalSessionAuthVerifier` (password-based session) and by `BasicAuthHeaderAutoLoginSupport.doLogin()` (step 4).

## Test-only OSGi components

`BasicAuthTestSetup` and `SAPTestSetup` mutate portal state at startup but must be inert in production. The pattern:

```java
@Component(
	configurationPid = "com.liferay.support.tools.basicauth.BasicAuthTestSetup",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true,
	service = {}
)
public class BasicAuthTestSetup {

	@Activate
	protected void activate() { /* mutate state */ }

	@Reference(target = "(module.service.lifecycle=portal.initialized)")
	private ModuleServiceLifecycle _moduleServiceLifecycle;

}
```

| Element | Purpose |
|---|---|
| `ConfigurationPolicy.REQUIRE` | Activates only when the matching `.config` exists — the test image has it, production does not |
| `service = {}` | Publishes nothing; it is a one-shot side effect |
| `immediate = true` | Activates without a consumer (there never is one) |
| `portal.initialized` reference | Runs after all property- and file-based configuration seeding |

The `.config` only needs to exist (e.g. `enabled=B"true"`). Keep it free of comments — see below.

## `.config` file syntax

Felix TypedProperties format:

| Type | Marker | Example |
|---|---|---|
| String | none or `S` | `name="hello"` |
| Boolean | `B` | `enabled=B"true"` |
| Integer | `I` | `count=I"5"` |
| Long | `L` | `id=L"100"` |
| Array | `[…]` | `tags=["a","b"]` |

- `enabled=true` without a marker is a String; a `boolean` attribute then falls back to its default. Always write `B"true"`.
- More than one comment line makes DXP 2026 reject the whole file ("Multiple comment lines found"). Keep `.config` files comment-free and document them in Java or here.
- Arrays are valid only for `String[]` attributes. `urlsIncludes` / `urlsExcludes` in every `BaseAuthVerifierConfiguration` are `String`: an array literal is stored as `[Ljava.lang.String;@<hash>`, matches no URL, and the verifier is skipped without any log line.
- `<pid>~<name>.config` is a factory instance; `-` works in place of `~` for legacy reasons (hence `…Configuration-default.config`).

## Liferay MCP Server

The design is [ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md). `McpServerSmokeSpec` is the regression guard, and `McpToolSetSpec` guards the `ldf-workflow` tool set ([Workflow API → MCP tool set](workflow-api.md#mcp-tool-set)).

- **Feature flag**: `feature.flag.LPD-63311=true` in `configs/common/portal-ext.properties`.
- **Per-instance switch**: OSGi PID `com.liferay.mcp.server.rest.internal.configuration.MCPServerConfiguration`, boolean `enabled` (default `false`, COMPANY scope). The system-level file `configs/common/osgi/configs/com.liferay.mcp.server.rest.internal.configuration.MCPServerConfiguration.config` (`enabled=B"true"`) is enough; a `configuration.override.` entry in `portal-ext.properties` is not needed.
- **Disabled state**: when the flag or `enabled` is off, `MCPServerAuthVerifierFilter` answers HTTP 404 on `/o/mcp`.
- **Requests**: `/o/mcp` accepts Basic auth. Send `Content-Type: application/json` and `Accept: application/json, text/event-stream`.
- **`forceBasicAuth` empties the tool list**: to build a profile's tools, `MCPServerServlet` fetches the tool set's OpenAPI document (`/mcp-server/v1.0/openapi.json` for the `default` profile) through an internal request that carries no `Authorization` header. With `forceBasicAuth=B"true"` ([step 2](#2-register-basic-auth-on-the-jsonws-servlet-filter)) that request gets HTTP 401, the log shows `Skipping MCP tool "getToolSetsPage" from tool set "mcp-server-v1.0" ... HTTP 401 for /mcp-server/v1.0/openapi.json`, and `tools/list` returns `{"tools":[]}` while `initialize` still succeeds. The fix is an `urlsExcludes` entry on the Basic Auth verifier for that document.
- **The meta tools read each tool set's OpenAPI document the same way**: `getToolSetToolSetNameToolSummariesPage`, `getToolSetToolSetNameTool` and `postToolSetToolSetNameToolInvoke` go through `ToolSetUtil._getOpenAPIJSONObject`, which fetches `/o/<tool set path>/openapi.json` without credentials. A tool set whose document is not excluded fails with `Unable to read the OpenAPI document of the "<tool set>" tool set`, although `getToolSetsPage` still lists it. `urlsExcludes` takes servlet-style patterns (exact paths, `/dir/*` prefixes, `*.ext` extensions); `*.json` would exempt every JSON path, so each tool set gets its own exact entry in the `-default` `.config` file ([step 2](#2-register-basic-auth-on-the-jsonws-servlet-filter)). Add one when the MCP surface needs another tool set. (liferay-portal master reads the document in-process; the HTTP fetch and its 401 were observed on the pinned DXP image for `mcp-server-v1.0`, and `McpServerSmokeSpec` locks the `headless-admin-user-v1.0` case.)
- **`forceBasicAuth` also breaks tool invocation**: `postToolSetToolSetNameToolInvoke` runs the tool through `VulcanRequestForwarder`, an internal forward that carries the MCP user as the `USER_ID` request attribute and no `Authorization` header. `VulcanRequestForwarderHttpServletRequestWrapperAuthVerifier` would accept it, but `forceBasicAuth` (translated to `basic_auth=true`) makes the Basic Auth verifier the only one `AuthVerifierPipeline` runs for its URLs (`_SUPREME_AUTH_VERIFIER_KEYS`), so the forward gets a Basic challenge and the tool returns `isError=true` with `Status code: 401`. Discovery and the summaries still work. For `ldf-workflow` the fix is the split in [step 2](#2-register-basic-auth-on-the-jsonws-servlet-filter): `/o/ldf-workflow/*` is excluded from the forced `-default` instance and covered by the `-ldfworkflow` instance with `forceBasicAuth=B"false"`, so the forward verifier can run. A wrong password on `/o/ldf-workflow` still gets HTTP 401 there ([When Basic Auth falls through to Guest](#when-basic-auth-falls-through-to-guest)). A request with **no** credentials reaches the resource as Guest, which is the stock DXP behavior. Before the fix, an unauthenticated `role.create` through `/execute` succeeded; how the resource now rejects Guest is in [Workflow API → Authentication](workflow-api.md#authentication). The headless lookup tool sets are not split yet. Invoking them through MCP in this container still returns 401 (observed: `getMyUserAccount` on `headless-admin-user-v1.0`). Split them the same way when the MCP surface needs them.
- **Caching**: the servlet caches the built tool list, and `ToolSetUtil` caches each OpenAPI document per company. Both caches are in memory, so a container restart clears them; a `configs/` change still needs `stopDockerContainer` + `startDockerContainer` because the files are baked into the image ([development guide](../guides/development.md)).

## Other runtime behaviour

- **Verifier cache.** After hot-editing `auth.verifier.*` properties and restarting Tomcat without rebuilding the image, Basic Auth may keep failing for about three minutes while `AuthVerifierPipeline` caches its configuration. Normal test runs start cold and never see this.
