# DXP 2026.Q1.3-LTS — Runtime and Environment Gotchas

L3 detail. Concrete pitfalls discovered during DXP 2026 migration. Read on demand from `.claude/rules/debugging.md`.

## 1. `portal-liferay-online-config.properties` baked into the DXP base image

**Symptom**: Every JSONWS call returns HTTP 403 or "Access denied" immediately after the container starts. No `configs/` change seems to help.

**Root cause**: `liferay/dxp:2026.q1.3-lts` bakes `/home/liferay/portal-liferay-online-config.properties` into the image. This file contains:

```
json.servlet.hosts.allowed=N/A
```

This value blocks all JSONWS access from any host, including `localhost`.

**Fix**: Place an empty file at `configs/common/portal-liferay-online-config.properties`. The workspace plugin merges `configs/common/` into `/home/liferay/` at image build time, and an empty file shadows the baked-in one.

```
# configs/common/portal-liferay-online-config.properties
# Intentionally empty — shadows the baked-in DXP file that sets
# json.servlet.hosts.allowed=N/A and blocks all JSONWS access.
```

**Verification**: After container start, run:

```bash
docker exec <container-name> cat /home/liferay/portal-liferay-online-config.properties
```

The output should be empty (or show only the comment). If it shows `json.servlet.hosts.allowed=N/A`, the shadow file was not deployed correctly.

## 2. BasicAuth `.config` `urlsIncludes` is a comma-separated String, not an array

**Symptom**: JSONWS calls using Basic Auth credentials silently fall through to the Guest user (HTTP 200 with Guest's data, or 403 from SAP), even though `BasicAuthHeaderAuthVerifierConfiguration.config` is deployed and contains `enabled=B"true"`.

**Root cause**: `BaseAuthVerifierConfiguration.urlsIncludes()` is declared as `String`, NOT `String[]`. The OSGi `.config` parser accepts the array literal `["/api/*","/o/*","/xmlrpc/*"]` without complaint, but it stores the value as `[Ljava.lang.String;@<hash>` (the default `toString` of a Java array). Liferay then treats that gibberish as a single literal pattern, no incoming URL matches, and the verifier is silently skipped.

**Correct file** (`configs/common/osgi/configs/com.liferay.portal.security.auth.verifier.internal.basic.auth.header.configuration.BasicAuthHeaderAuthVerifierConfiguration-default.config`):

```
enabled=B"true"
urlsIncludes="/api/*,/o/*,/xmlrpc/*"
forceBasicAuth=B"true"
```

Notes:
- DXP 2026 keeps the JSONWS base at `/api/jsonws/`. There is no `/portal/api/*` endpoint to register.
- `forceBasicAuth=B"true"` does not change BasicAuth behavior; it surfaces a wrong-credentials failure as **HTTP 401** instead of the silent Guest fallback (`_getBasicUserId` catches `AuthException` and returns 0). Keep it on for visibility — without it, the most common BasicAuth failure mode (wrong password) looks like a successful Guest call and produces a downstream 403 from SAP that masks the real cause.
- The same comma-separated rule applies to `urlsExcludes` and to every `@AttributeDefinition` returning `String`. See `docs/details/api-liferay-dxp2026.md` §15.

A separate registration is required for `/api/jsonws/*` because that path is served by the JSON Web Service Servlet Filter, not the OSGi AuthVerifierFilterTracker. See §12 below.

## 3. AuthVerifierPipeline cache delay (~3 minutes)

**Symptom**: After updating `auth.verifier.*` properties in `portal-ext.properties` and restarting Tomcat (without rebuilding the image), Basic Auth requests still fail for up to 3 minutes.

**Root cause**: The `AuthVerifierPipeline` caches its configuration. The cache clears after approximately 3 minutes.

**Note**: Not typically observed during test runs because the container is disposable per run; a fresh AuthVerifierPipeline starts cold each time. It is noted here for cases where `portal-ext.properties` is hot-updated during debugging.

## 4. License activation: readiness polling watch-point

License activation races the first HTTP request. Current implementation uses a simple HTTP 200/302 readiness poll (see `awaitLiferayReady` in `integration-test/build.gradle`). If DXP 2026's license-activation sequence ever redirects to `/portal/update_language` or similar, extend the polling to inspect the `Location` header.

## 5. `Accept-Encoding: identity` must be forced on HTTP test requests

**Symptom**: `BaseLiferaySpec` helper methods occasionally return garbled JSON or trigger `JsonParseException` when the Liferay server returns gzip-compressed responses.

**Root cause**: Java's `HttpURLConnection` does not automatically decompress `Content-Encoding: gzip` responses unless the `java.net.http.HttpClient` API is used. The Groovy `URL.openConnection()` pattern used in test helpers receives the compressed bytes and tries to parse them as JSON.

**Fix**: Set `Accept-Encoding: identity` on all outbound test HTTP requests:

```groovy
conn.setRequestProperty('Accept-Encoding', 'identity')
```

This tells the server not to compress the response. Applied in `BaseLiferaySpec._httpGet` and `_httpPost`.

## 6. Workspace plugin ≥16 requires explicit `include` for all subprojects in `settings.gradle`

**Symptom**: `:modules:liferay-dummy-factory:jar` task is not found; Gradle reports "project ':modules:liferay-dummy-factory' not found".

**Root cause**: Workspace plugin 16.x no longer auto-discovers subprojects. Every subproject must be declared explicitly.

**Fix** in `settings.gradle`:

```groovy
include 'integration-test'
include 'modules:liferay-dummy-factory'
```

Both lines are required. Without the `modules:liferay-dummy-factory` line, the bundle JAR is never built and `integrationTest` fails immediately.

## 7. Headless organizations `?search=` has Elasticsearch indexing lag

**Symptom**: `GET /o/headless-admin-user/v1.0/organizations?search=<name>` returns an empty list immediately after a create call, even though the organization was created successfully.

**Root cause**: The `?search=` parameter routes through Elasticsearch. There is an observable indexing lag (typically 1–5 seconds, but potentially longer under load) between when the entity is persisted and when it appears in search results.

**Fix**: Drop `?search=` and use `?pageSize=100` instead, then filter client-side by name. For post-condition assertions in specs, always use the page-and-filter pattern:

```groovy
List orgs = jsonwsGet(
    "organization/get-organizations" +
    "/company-id/${companyId}/parent-organization-id/0/start/0/end/100"
) as List

def match = orgs.find { it.name == expectedName }
assert match != null
```

## 8. Fixed Docker port conflict warning

The workspace plugin always binds fixed ports:

| Port | Service |
|------|---------|
| 8080 | HTTP |
| 11311 | GoGo Shell |
| 8000 | JPDA (debug) |

If another Liferay instance (or any other process) is already bound to port 8080 or 11311, `startDockerContainer` will fail with "port is already allocated".

**Before running `./gradlew startDockerContainer`**:

```bash
docker ps                # check for running containers
lsof -i :8080            # check for any process on 8080
lsof -i :11311           # check for any process on 11311
```

Stop any conflicting process or container first. There is no configuration option to change these ports without modifying the workspace plugin task directly.

## 9. Admin first-login still redirects to `/c/portal/update_password`

`configs/common/portal-ext.properties` carries:

```properties
company.security.update.password.required=false
passwords.default.policy.change.required=false
```

These suppress the legacy `PASSWORDRESET` HTTP ticket flow, but **DXP 2026 still redirects the default admin to `/c/portal/update_password` on first browser login**. The redirect happens after the form POST to `/c/portal/login` succeeds; the session is authenticated, but the next navigation hits an interstitial form. The legacy ticket flow is gone, but the password change UI remains.

`BaseLiferaySpec.loginAsAdmin` therefore handles the form directly: if the post-login URL contains `/c/portal/update_password`, it fills `#password1` / `#password2` with `NEW_ADMIN_PASSWORD = 'Test12345'` and submits.

```groovy
if (page.url().contains('/c/portal/update_password')) {
    page.locator('#password1').fill(NEW_ADMIN_PASSWORD)
    page.locator('#password2').fill(NEW_ADMIN_PASSWORD)
    page.locator('button[type=submit]').click()
}
```

`loginAsAdmin` also tries credentials in order — `DEFAULT_ADMIN_PASSWORD` first, then `NEW_ADMIN_PASSWORD` — so a spec that runs after the form has already been submitted in another spec still logs in successfully. The `LdfResourceClient.login` helper performs the same fallback.

After the password is changed, `BaseLiferaySpec.basicAuthHeader()` MUST use `NEW_ADMIN_PASSWORD`, not the original `DEFAULT_ADMIN_PASSWORD`. The default password silently fails (`_getBasicUserId` catches `AuthException` and returns 0 → falls through to Guest), and the symptom is a 403 from SAP rather than a clear 401. See §10 for the silent-fallback discussion and §2 for the `forceBasicAuth=B"true"` mitigation.

## 10. BasicAuth wrong-password failures fall through silently to the Guest user

**Symptom**: A JSONWS call with BasicAuth returns Guest's data or a SAP-denied 403, even though the credentials are correct in production. Adding diagnostic logging shows `Liferay.authToken` from the spec belongs to the Guest user (`20099`).

**Root cause**: `BasicAuthHeaderAutoLoginSupport._getBasicUserId(...)` catches `AuthException` and returns 0:

```java
// Liferay portal source — paraphrased
try {
    long userId = userLocalService.authenticateForBasic(...);
    return userId;
}
catch (AuthException ae) {
    return 0;  // Guest
}
```

A return of 0 means "no authenticated user", which the AutoLogin pipeline interprets as Guest. There is no log line, no warning, no surfaced exception. The most common trigger is a stale password on the test side after DXP 2026's first-login `/c/portal/update_password` redirect (§9).

**Mitigations**:
1. `forceBasicAuth=B"true"` in `BasicAuthHeaderAuthVerifierConfiguration-default.config` (see §2). This causes the verifier to return `null` rather than a Guest result, which the filter then translates into HTTP 401 instead of letting the request continue as Guest.
2. Match `basicAuthHeader()` to whatever password is actually in effect (post-update_password = `NEW_ADMIN_PASSWORD`).
3. Enable the BasicAuth login flow itself (§11) — without it, even correct credentials cannot reach `passwordBasedAuthentication=true` and downstream SAP enforcement fails.

The `forceBasicAuth → basic_auth` filter property mapping is performed by `BasicAuthHeaderAuthVerifierPipelineConfigurator.translateKey(...)`. If you grep for `basic_auth=true` in portal source and find nothing, it is because the property name is renamed during translation.

## 11. `BasicAuthHeaderSupportConfiguration.enabled=false` is hardened in `portal-liferay-online-config.properties`

**Symptom**: BasicAuth credentials are accepted (no 401) but `passwordBasedAuthentication` is never set on the AuthVerifierResult. `SYSTEM_USER_PASSWORD` SAP entries never fire and only services in `SYSTEM_DEFAULT`'s narrow allowlist (Country, Region) are callable. An OSGi `.config` override at
`configs/common/osgi/configs/com.liferay.portal.security.configuration.BasicAuthHeaderSupportConfiguration.config` with `enabled=B"true"` has no effect — `getCompanyConfiguration` still returns `enabled=false`.

**Root cause**: DXP 2026's image ships `/home/liferay/portal-liferay-online-config.properties` with:

```
configuration.override.com.liferay.portal.security.configuration.BasicAuthHeaderSupportConfiguration_enabled=B"false"
```

The OSGi configuration source priority chain in DXP 2026, lowest to highest, is:

1. `portal.properties` defaults
2. `portal-ext.properties` `configuration.override.*` entries
3. `osgi/configs/<pid>.config` files
4. `portal-liferay-online-config.properties` `configuration.override.*` entries
5. Runtime `ConfigurationAdmin.update(...)` writes

`portal-liferay-online-config.properties` is processed **after** `portal-ext.properties`, so a `configuration.override.*` entry in user-supplied portal-ext **cannot win**. The `.config` file at level 3 is also overwritten by level 4 on every restart. Only level 5 — a runtime mutation via `ConfigurationAdmin.update(...)` — survives.

**Fix**: Write the override programmatically after `portal.initialized`. Reference: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/basicauth/BasicAuthTestSetup.java`.

```java
@Activate
protected void activate() {
    Configuration configuration = _configurationAdmin.getConfiguration(
        "com.liferay.portal.security.configuration." +
            "BasicAuthHeaderSupportConfiguration",
        "?");

    Dictionary<String, Object> properties = new Hashtable<>();
    // ...copy existing properties...
    properties.put("enabled", Boolean.TRUE);
    configuration.update(properties);
}

@Reference(target = "(module.service.lifecycle=portal.initialized)")
private ModuleServiceLifecycle _moduleServiceLifecycle;
```

`BasicAuthHeaderAutoLoginSupport` reads its configuration freshly on every request via `ConfigurationProvider.getCompanyConfiguration`, so the override takes effect on the next BasicAuth attempt without an `@Modified` callback.

This same priority-chain rule applies to **any** configuration the DXP 2026 image hardens via `portal-liferay-online-config.properties`. To find what is hardened, run:

```bash
docker exec <container-name> cat /home/liferay/portal-liferay-online-config.properties \
    | grep configuration.override
```

## 12. JSON Web Service Servlet Filter is a separate AuthVerifierFilter from the OSGi pipeline

**Symptom**: BasicAuth works for `/o/headless-*` and `/o/c/<entity>` paths, but every call to `/api/jsonws/*` falls through to Guest — even with the BasicAuth `.config` from §2 deployed correctly.

**Root cause**: `/api/jsonws/*` is served by the **JSON Web Service Servlet** (declared in `shielded-container-web.xml` / `liferay-web.xml`), which has its own `AuthVerifierFilter` instance with init param `portal_property_prefix=jsonws.servlet.`. The generic OSGi `AuthVerifierFilterTracker` pipeline (configured by `BasicAuthHeaderAuthVerifierConfiguration`) does not run on this path — it covers `/o/*` and a few other servlets, not the JSONWS servlet.

The JSONWS servlet filter reads its verifier registrations from portal properties prefixed with `jsonws.servlet.` (`AuthVerifierFilter.java:68-79`):

```
jsonws.servlet.auth.verifier.<VerifierClassName>.urls.includes=...
jsonws.servlet.auth.verifier.<VerifierClassName>.<key>=...
```

**Fix** in `configs/common/portal-ext.properties`:

```properties
jsonws.servlet.auth.verifier.BasicAuthHeaderAuthVerifier.urls.includes=*
jsonws.servlet.auth.verifier.BasicAuthHeaderAuthVerifier.basic_auth=true
jsonws.servlet.auth.verifier.PortalSessionAuthVerifier.urls.includes=*
```

`urls.includes=*` is sufficient because the filter is already mounted at `/api/jsonws/*` via the servlet mapping. The `PortalSessionAuthVerifier` line lets browser-session-authenticated callers (Playwright sessions, the portlet UI) keep using JSONWS without a BasicAuth header.

Note: the property uses dotted form (`urls.includes`) here because it is a portal property, not an OCD attribute. The `.config` file at §2 uses camelCase (`urlsIncludes`) because that maps to the OCD method name. The two are intentionally different.

## 13. JSONWS BasicAuth requests need CSRF token bypass

**Symptom**: `/api/jsonws/*` POST calls with valid BasicAuth credentials return HTTP 403 with body `PrincipalException$MustHaveSessionCSRFToken`.

**Root cause**: `JSONWebServiceServiceAction` is a `JSONAction` subclass, and `JSONAction.checkAuthToken(...)` requires the session to carry a CSRF token. `SessionAuthToken.checkCSRFToken` throws `MustHaveSessionCSRFToken` when no token is present. BasicAuth-only callers never establish a session, so they never have a CSRF token to send.

**Fix** in `configs/common/portal-ext.properties`:

```properties
auth.token.ignore.origins=com.liferay.portal.remote.json.web.service.web.internal.JSONWebServiceServiceAction
```

This is equivalent to `auth.token.check.enabled=false` but scoped to a single origin (the JSONWS service action). All other `JSONAction` subclasses still enforce CSRF.

If multiple origins need bypassing, use a comma-separated list — the property is read by `AuthTokenWhitelistImpl` as a comma-separated set.

## 14. Test-only OSGi components: `ConfigurationPolicy.REQUIRE` + `ModuleServiceLifecycle` reference

**Pattern**: An OSGi component that mutates portal state (writes `ConfigurationAdmin`, calls `*LocalService.update*`) at test time, but is invisible in production.

```java
@Component(
    configurationPid = "com.example.MySetup",
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    immediate = true,
    service = {})
public class MySetup {

    @Activate
    protected void activate() { /* mutate state */ }

    @Reference(target = "(module.service.lifecycle=portal.initialized)")
    private ModuleServiceLifecycle _moduleServiceLifecycle;
}
```

Why each piece:

- `ConfigurationPolicy.REQUIRE` — the component only activates when a `.config` file matching `configurationPid` is present. The test harness deploys the `.config`; production deploys do not. The same JAR is safe to ship to either environment.
- `service = {}` — the component publishes nothing into the OSGi service registry. It is a one-shot side-effect on activation, not a queryable service.
- `@Reference(target = "(module.service.lifecycle=portal.initialized)")` — activation is gated until after portal initialization. This is strictly later than all property-file-based configuration seeding (`.config`, online-config, portal-ext), so any state we mutate is mutated **after** the platform's own seed pass.
- `immediate = true` — without it the component would wait for a service request before activating, which never comes (because `service = {}`).

The `.config` file content can be minimal — even just `enabled=B"true"` — its only role is to satisfy the `REQUIRE` policy. Comments are NOT allowed: DXP 2026's TypedProperties parser raises "Multiple comment lines found" if the `.config` contains more than one comment line. Keep `.config` files for `REQUIRE`-gated components free of inline comments; document the file in the matching Java source instead.

References: `BasicAuthTestSetup.java`, `SAPTestSetup.java`, and their `.config` siblings under `configs/common/osgi/configs/`.

## 15. `.config` file syntax: `B"true"`, `S"value"`, comma-separated

The Felix TypedProperties format used by `.config` files prefixes literals with a one-character type marker:

| Type    | Marker | Example                          |
|---------|--------|----------------------------------|
| String  | (none) or `S` | `name="hello"` or `name=S"hello"` |
| Boolean | `B`    | `enabled=B"true"`                |
| Integer | `I`    | `count=I"5"`                     |
| Long    | `L`    | `id=L"100"`                      |
| Array   | `[...]` | `tags=["a","b","c"]`            |

Common pitfalls:

- `enabled=true` (no marker) is parsed as a String, NOT a Boolean. The OCD method `boolean enabled()` then sees a `String` value and either fails to coerce or, worse, takes the default. Always write `enabled=B"true"` for boolean attributes.
- DXP 2026's TypedProperties parser raises **"Multiple comment lines found"** if a `.config` file contains more than one comment line. Some earlier Liferay versions silently ignored the second comment; DXP 2026 errors out and the entire file is rejected. Keep `.config` files comment-free or use a single leading comment only.
- Array literals (`["a","b"]`) are valid only when the OCD attribute returns `String[]`. For `String` attributes (e.g. `urlsIncludes`), the literal serializes as `[Ljava.lang.String;@<hash>` — see §2 and `docs/details/api-liferay-dxp2026.md` §15.

Factory configurations use the suffix `~<instance>` (e.g. `<pid>~default.config`). Non-factory configs use `<pid>.config`. The `-default` suffix used in this project's BasicAuth file (`...BasicAuthHeaderAuthVerifierConfiguration-default.config`) is a singleton instance of a factory PID — Liferay treats `-` and `~` interchangeably in this position for legacy reasons.
