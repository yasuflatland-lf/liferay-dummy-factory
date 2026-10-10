# DXP runtime configuration reference

How the test container is configured so that JSONWS, Basic Auth and the admin login work, and the Liferay behaviours that forced each setting. Everything here lives in `configs/common/` (baked into the image by `dockerDeploy`) or in the test-only components `BasicAuthTestSetup` / `SAPTestSetup`.

If JSONWS calls fail with 401/403 or come back as the Guest user, start at [Basic Auth, step by step](#basic-auth-step-by-step).

## Files in `configs/common/`

| File | Why it exists |
|---|---|
| `portal-ext.properties` | Disables setup wizard / terms / reminder queries / password-change prompts; opens JSONWS; registers Basic Auth on the JSONWS servlet; exempts JSONWS from CSRF; widens the default SAP |
| `portal-liferay-online-config.properties` | **Intentionally empty** — shadows the image's file that blocks JSONWS |
| `osgi/configs/…BasicAuthHeaderAuthVerifierConfiguration-default.config` | Enables Basic Auth on the OSGi verifier pipeline (`/o/*`, `/api/*`) |
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
| 5 | The password sent is the current admin password | silent Guest fallback, then a SAP 403 | `NEW_ADMIN_PASSWORD` after the first-login form |

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
urlsExcludes="/o/mcp-server/v1.0/openapi.json,/o/headless-admin-site/v1.0/openapi.json,/o/headless-admin-taxonomy/v1.0/openapi.json,/o/headless-admin-user/v1.0/openapi.json"
forceBasicAuth=B"true"
```

`forceBasicAuth` turns a wrong password into an HTTP 401 instead of a silent Guest request (Liferay translates it to the `basic_auth` filter property, which is why grepping for `basic_auth=true` finds nothing in the OCD). `urlsIncludes` is a **comma-separated String** — see [`.config` syntax](#config-file-syntax). `urlsExcludes` exists only for the Liferay MCP Server — see [Liferay MCP Server](#liferay-mcp-server).

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

After that, Basic Auth must use the new password; `basicAuthHeader()` is hard-wired to `NEW_ADMIN_PASSWORD`. A wrong password is swallowed by `BasicAuthHeaderAutoLoginSupport._getBasicUserId`, which catches `AuthException` and returns `0` — Guest — with no log line. `forceBasicAuth` (step 2) is what makes it a visible 401.

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

The design is [ADR-0010](../adr/0010-mcp-via-liferay-mcp-server.md); `McpServerSmokeSpec` is the regression guard.

- **Feature flag**: `feature.flag.LPD-63311=true` in `configs/common/portal-ext.properties`.
- **Per-instance switch**: OSGi PID `com.liferay.mcp.server.rest.internal.configuration.MCPServerConfiguration`, boolean `enabled` (default `false`, COMPANY scope). The system-level file `configs/common/osgi/configs/com.liferay.mcp.server.rest.internal.configuration.MCPServerConfiguration.config` (`enabled=B"true"`) is enough; a `configuration.override.` entry in `portal-ext.properties` is not needed.
- **Disabled state**: when the flag or `enabled` is off, `MCPServerAuthVerifierFilter` answers HTTP 404 on `/o/mcp`.
- **Requests**: `/o/mcp` accepts Basic auth. Send `Content-Type: application/json` and `Accept: application/json, text/event-stream`.
- **`forceBasicAuth` empties the tool list**: to build a profile's tools, `MCPServerServlet` fetches the tool set's OpenAPI document (`/mcp-server/v1.0/openapi.json` for the `default` profile) through an internal request that carries no `Authorization` header. With `forceBasicAuth=B"true"` ([step 2](#2-register-basic-auth-on-the-jsonws-servlet-filter)) that request gets HTTP 401, the log shows `Skipping MCP tool "getToolSetsPage" from tool set "mcp-server-v1.0" ... HTTP 401 for /mcp-server/v1.0/openapi.json`, and `tools/list` returns `{"tools":[]}` while `initialize` still succeeds. The fix is an `urlsExcludes` entry on the Basic Auth verifier for that document.
- **The meta tools read each tool set's OpenAPI document the same way**: `getToolSetToolSetNameToolSummariesPage`, `getToolSetToolSetNameTool` and `postToolSetToolSetNameToolInvoke` go through `ToolSetUtil._getOpenAPIJSONObject`, which fetches `/o/<tool set path>/openapi.json` without credentials. A tool set whose document is not excluded fails with `Unable to read the OpenAPI document of the "<tool set>" tool set`, although `getToolSetsPage` still lists it. `urlsExcludes` takes exact paths or trailing-`*` prefixes only, so each tool set gets its own entry. The current list covers `mcp-server`, `headless-admin-site`, `headless-admin-taxonomy` and `headless-admin-user` (the lookup tool sets ADR-0010 relies on); add the entry when the MCP surface needs another tool set. (liferay-portal master reads the document in-process; the HTTP fetch and its 401 were observed on the pinned DXP image for `mcp-server-v1.0`, and `McpServerSmokeSpec` locks the `headless-admin-user-v1.0` case.)
- **Caching**: the servlet caches the built tool list, and `ToolSetUtil` caches each OpenAPI document per company. Both caches are in memory, so a container restart clears them; a `configs/` change still needs `stopDockerContainer` + `startDockerContainer` because the files are baked into the image ([development guide](../guides/development.md)).

## Other runtime behaviour

- **Verifier cache.** After hot-editing `auth.verifier.*` properties and restarting Tomcat without rebuilding the image, Basic Auth may keep failing for about three minutes while `AuthVerifierPipeline` caches its configuration. Normal test runs start cold and never see this.
