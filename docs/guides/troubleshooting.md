# Troubleshooting

Symptom → cause → fix. Find the root cause before changing anything: no fallbacks that hide the failure, no skipped checks, no `--no-verify`. If you meet unexpected files, branches or config, investigate before deleting — it may be someone's work in progress.

## Inspecting the container

```bash
docker ps -a --filter name=liferay                       # state of the container
docker logs -f liferay-dummy-factory-liferay              # portal log
docker exec liferay-dummy-factory-liferay bash -c \
  "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"   # bundle state
```

More GoGo shell commands (same `telnet localhost 11311` session):

| Command | Shows |
|---|---|
| `lb dummy.factory` | Bundle state: `Active` is good, `Installed`/`Resolved` means it did not start |
| `diag <bundle-id>` | Unresolved imports of a bundle |
| `headers <bundle-id>` | Manifest, e.g. that `Import-Package` has `jakarta.portlet` and no `javax.servlet` |
| `scr:info <component FQCN>` | Component state; look for `UNSATISFIED REFERENCE` |
| `services jakarta.portlet.Portlet` | Whether the portlet service is registered |

Query data directly instead of through the UI:

```bash
curl -u test@liferay.com:Test12345 \
  "http://localhost:8080/api/jsonws/user/get-current-user"
```

(`Test12345` once the first browser login has changed the admin password, `test` before.)

## Bundle and portlet

| Symptom | Cause | Fix |
|---|---|---|
| Bundle not `Active` although it compiles | It imports `javax.servlet` | Keep `Import-Package: !javax.servlet,!javax.servlet.http,*` in `bnd.bnd` ([why](../reference/liferay-dxp-api.md#bndbnd-must-exclude-javaxservlet)) |
| Bundle resolution fails after editing a JSP | Taglib URI changed to `jakarta.tags.portlet` | Keep `http://xmlns.jcp.org/portlet_3_0` |
| Portlet missing from the Control Panel, PanelApp `UNSATISFIED REFERENCE` | Portlet registered under the wrong namespace / not picked up by the portlet tracker | Check `scr:info` on the PanelApp and portlet, and `services jakarta.portlet.Portlet` |
| `compileJava` suddenly fails with no local change | `release.dxp.api` floated to a newer release | Remove any `version: "default"` ([why](../reference/liferay-dxp-api.md#releasedxpapi-never-pin-version-default)) |
| UI shows raw keys like `create-user` | Key missing from `Language.properties`, or the resource bundle is not injected | Add the key; check [i18n](../architecture/frontend.md#i18n-two-resolution-paths) |
| "Could not find resource URL" | New resource command not registered in `view.jsp` | [Register it](adding-an-entity.md#2-resource-command-portletactions) |
| Workflow buttons stay disabled, no error shown | Schema fetch to `/o/ldf-workflow` got 401 (missing CSRF header) | Send `credentials` **and** `x-csrf-token` ([why](../architecture/frontend.md#server-communication)) |
| New user does not appear in Control Panel → Users | `addUserWithWorkflow` got a `type` other than `TYPE_REGULAR` | [Fix the argument](../reference/liferay-dxp-api.md#userlocalserviceadduserwithworkflow--int-type-at-position-20) |

## Container and Gradle

| Symptom | Cause | Fix |
|---|---|---|
| `DXP activation key not found` | No license variable | Set `LIFERAY_DXP_LICENSE_FILE` or `LIFERAY_DXP_LICENSE_BASE64` |
| `startDockerContainer` fails with "port is already allocated" | Another process on 8080/11311/8000/6300 | `docker ps`, `lsof -i :8080`; stop it |
| `startDockerContainer` fails with `Status 304:` and an empty body | The container is already running (often after a half-failed start) | `./gradlew stopDockerContainer` then retry; if the state is corrupt, `removeDockerContainer` |
| `project ':modules:liferay-dummy-factory' not found` | Subproject not in `settings.gradle` | Workspace plugin requires explicit `include` lines |
| Integration tests "pass" in seconds | Gradle replayed a cached result | Clean and rerun ([details](testing.md#trust-the-result)) |
| A test passes locally but not in CI (or vice versa) | State left in the kept volume | `./gradlew removeDockerContainer` and rerun |
| Specs compile "fine" but break in CI | You ran `compileGroovy` (no sources) | Use `:integration-test:compileTestGroovy` |
| Integration coverage missing, CI step "JaCoCo XML missing" | Agent on 6300 unreachable or no spec ran | [Coverage pitfalls](../reference/test-harness.md#integration-tests) |

## JSONWS and authentication

| Symptom | Cause | Fix |
|---|---|---|
| Every JSONWS call 403 from the first test | Image's online-config blocks JSONWS | [Step 1](../reference/dxp-runtime-config.md#1-shadow-the-online-config-file) |
| Basic Auth works on `/o/*` but JSONWS runs as Guest | JSONWS servlet has its own verifier | [Step 2](../reference/dxp-runtime-config.md#2-register-basic-auth-on-the-jsonws-servlet-filter) |
| POST 403 `MustHaveSessionCSRFToken` | JSONWS CSRF check | [Step 3](../reference/dxp-runtime-config.md#3-exempt-jsonws-from-the-csrf-check) |
| Only Country/Region services callable | Basic Auth login support disabled or SAP not widened | [Step 4](../reference/dxp-runtime-config.md#4-enable-basic-auth-login-support-at-runtime), [SAP](../reference/dxp-runtime-config.md#service-access-policies-sap) |
| 401 with the default password | Admin password changed by the first-login form | [Step 5](../reference/dxp-runtime-config.md#5-use-the-current-admin-password) |
| `/api/jsonws/company/*` 404 | `CompanyService` is blacklisted | Read `companyId` from `user/get-current-user` |
| Module service path 404 | Missing context prefix | e.g. `blogs.blogsentry/...` ([table](../reference/liferay-dxp-api.md#what-is-exposed)) |
| Garbled JSON / parse errors in helpers | gzip response | Send `Accept-Encoding: identity` (the harness does) |

## Playwright

| Symptom | Cause | Fix |
|---|---|---|
| Create test green although the server failed | Waited on `<entity>-result` without `.alert-success` | [Assert success](../reference/playwright.md#assert-success-not-presence) |
| Wait on `<option>` or progress bar times out | Element is never "visible" | Use `ATTACHED` |
| Click works but the response never comes | `setForce(true)` on a disabled button | Wait for `:not([disabled])` |
| `ReferenceError: Liferay is not defined` | Global loads asynchronously | Poll with `waitForFunction` |
| Strict-mode violation on a tab | `:has-text` substring match | Use `data-testid` |
| Text assertion passes with a missing key | Asserted on the key | Assert on the resolved English text |
| Uppercase mismatch on a badge | `innerText()` under `text-transform` | Use `textContent()` |
| Search returns nothing right after create | `?search=` is Elasticsearch-backed | `?pageSize=100` and filter |

## MCP

| Symptom | Cause | Fix |
|---|---|---|
| `/o/mcp` returns 404 | MCP Server disabled | [Liferay MCP Server](../reference/dxp-runtime-config.md#liferay-mcp-server) |
| `/o/mcp/ldf` returns 404 | The `ldf` profile is missing | Re-run `./gradlew :integration-test:setupMcpProfile` |
| `setupMcpProfile` fails with `HTTP 401` | Stale admin password | Pass the current one with `-Pldf.password=...` ([task](../reference/test-harness.md#mcp-profile-task-setupmcpprofile)) |
| `setupMcpProfile` fails with `Authenticated as ..., expected ...` | The request did not run as `-Pldf.user`: the value is not that user's email, or (on a Liferay without forced Basic Auth) the credentials were ignored and it ran as Guest | [When Basic Auth falls through to Guest](../reference/dxp-runtime-config.md#when-basic-auth-falls-through-to-guest) |
| New or changed tools not visible after redeploying the bundle | Liferay caches each tool set's OpenAPI document | "Stale definitions after a redeploy" in [MCP tool set](../reference/workflow-api.md#mcp-tool-set) |
| Workflow returns HTTP 403 `FORBIDDEN`, or an MCP tool call returns `isError` with `Status code: 403` | The caller is not a company administrator (or not an omniadmin for `company.create`) | The `error` message and the server's WARN line name the reason; use credentials that meet the [authorization rule](../reference/workflow-api.md#authentication). |
| Tool call returns `isError` with `Status code: 422` | The step failed | Read `error` in the tool result. |
