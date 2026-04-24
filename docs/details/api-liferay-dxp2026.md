# Liferay DXP 2026.Q1.3-LTS — API notes

L3 detail. Non-obvious API facts for DXP 2026.Q1.3-LTS. This file is the single source of truth for DXP 2026 API constraints. It replaces `api-liferay-ce74.md` (deleted). Read on demand from `.claude/rules/writing-code.md` or `.claude/rules/debugging.md`.

## 1. `release.dxp.api` BOM replaces individual API dependencies

DXP 2026 ships a managed BOM artifact. Use it as the single dependency for all Liferay APIs in `modules/liferay-dummy-factory/build.gradle`:

```groovy
compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "default"
```

The BOM includes journals (`com.liferay.journal.api`), DDM (`com.liferay.dynamic.data.mapping.api`), message boards (`com.liferay.message.boards.api`), blogs, vocabulary/category, and all portal-kernel artifacts at the correct version. Adding individual API dependencies alongside `release.dxp.api` causes version skew and runtime `ClassCastException` or `NoClassDefFoundError`. Do not add per-API entries.

## 2. `GroupLocalService.addGroup` — new 17-argument signature

DXP 2026 adds `externalReferenceCode` as the first argument and `typeSettings` after `type`:

```java
// Before (CE 7.4 GA132) — 15 args
_groupLocalService.addGroup(
    userId, parentGroupId, className, classPK,
    liveGroupId, nameMap, descriptionMap, type,
    manualMembership, membershipRestriction, friendlyURL,
    site, inheritContent, active,
    serviceContext);

// After (DXP 2026) — 17 args
_groupLocalService.addGroup(
    externalReferenceCode,           // NEW: pass null for auto-generated ERC
    userId, parentGroupId, className, classPK,
    liveGroupId, nameMap, descriptionMap, type,
    typeSettings,                    // NEW: pass null or StringPool.BLANK for defaults
    manualMembership, membershipRestriction, friendlyURL,
    site, inheritContent, active,
    serviceContext);
```

Verify the exact signature against the source:
`/home/yasuflatland/tmp/liferay-portal/portal-kernel/src/com/liferay/portal/kernel/service/GroupLocalService.java`

Affected file: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/service/SiteCreator.java`.

## 3. `CompanyService` is blacklisted from JSON-WS

`portal.properties` lists `com.liferay.portal.kernel.service.CompanyServiceUtil` in `json.service.invalid.class.names`. Every path under `/api/jsonws/company/*` (and the legacy `/api/jsonws/company/*`) returns HTTP 404 regardless of method or parameters.

For tests that need the current company ID, use `user/get-current-user` and extract the `.companyId` field:

```groovy
// BaseLiferaySpec.getCompanyId()
def json = jsonwsGet('user/get-current-user')
return json.companyId as long
```

## 4. `CompanyLocalService.addCompany` — 13-argument overload

```java
addCompany(Long companyId, String webId, String virtualHostname, String mx,
           int maxUsers, boolean active, boolean addDefaultAdminUser,
           String defaultAdminPassword, String defaultAdminScreenName,
           String defaultAdminEmailAddress, String defaultAdminFirstName,
           String defaultAdminMiddleName, String defaultAdminLastName)
```

No simpler overload exists on `CompanyLocalService`. The shorter overload lives on `CompanyService`, which is blacklisted (see #3). For dummy company creation, pass `addDefaultAdminUser=false` and all admin fields as `null`. Reference: `CompanyCreator.java`.

## 5. `OrganizationService.addOrganization` — JSONWS availability on DXP 2026

`OrganizationService.addOrganization` remains exposed via `/api/jsonws/organization/add-organization` on DXP 2026, but Headless Admin User is the recommended path; verify behavior against a running container before relying on either endpoint.

Headless endpoint for reference:

```
POST /o/headless-admin-user/v1.0/organizations
Authorization: Basic <base64>
Content-Type: application/json
{"name": "...", "organizationType": "Organization"}
```

For verification (read), use `GET /o/headless-admin-user/v1.0/organizations?pageSize=100` and filter client-side by name. Avoid `?search=` for immediate post-create assertions — it routes through Elasticsearch and has an observable indexing lag.

## 6. `MBThreadLocalService.getThreads` — `categoryId` is an exact-match filter

The `categoryId` parameter is an exact-match filter, not a "no filter" sentinel. Passing `categoryId=0L` returns only threads whose parent category ID is exactly 0 (root-level threads), not all threads in the group.

To list all threads regardless of category, iterate `MBCategoryLocalService.getCategories(groupId)` and union per-category results with the root-level call. No single overload returns group-wide threads in one shot.

## 7. `MBCategoryLocalService.addCategory` — 5-argument overload removed in DXP 2026

The 5-argument overload (without `externalReferenceCode`) no longer exists in DXP 2026; use the 6-argument form for the typical case:

```java
addCategory(String externalReferenceCode, long userId, long parentCategoryId,
            String name, String description, ServiceContext serviceContext)
```

Pass `externalReferenceCode=null` and `parentCategoryId=0L` for top-level categories. A full-featured overload for advanced scenarios also exists — consult the source if additional parameters are needed.

## 8. `DefaultScreenNameValidator` accepts only `[a-zA-Z0-9._-]`

`com.liferay.portal.kernel.security.auth.DefaultScreenNameValidator` rejects any screen name outside this character class. It also rejects email-address form and reserved words such as `postfix`. The error surfaces as `UserScreenNameException.MustValidate`.

Practical consequences:

- Names from Datafaker locales with apostrophes (`O'Conner`), whitespace (`Mary Ann`), or non-ASCII (漢字) will fail.
- Purely numeric screen names are rejected by a separate rule; a sanitizer that strips all letters must then append a prefix.
- The validator does not lowercase; the caller must lowercase before passing.

Use `com.liferay.support.tools.utils.ScreenNameSanitizer` for external-generated name sources (#9 below).

## 9. `com.liferay.support.tools.utils.ScreenNameSanitizer`

Pure static utility for converting arbitrary text into a Liferay-legal screen name:

```java
public static String sanitize(String input);
```

Behavior:
1. `null` input → returns `"user"` (logged at WARN).
2. Strips everything outside `[a-zA-Z0-9._-]`.
3. Collapses consecutive `.` into a single `.`.
4. Strips leading/trailing `._-`.
5. Returns `"user"` as a fallback if the result is empty (logged at WARN).

The caller is responsible for lowercasing and for appending any disambiguating index suffix. Use this for **external-generated** names (Datafaker, RNGs, third-party APIs). For **user-supplied** names (e.g. `baseName` from a portlet form), validate and reject at the resource-command boundary — silently rewriting user input is a UX regression.

## 10. `ResourceCommandUtil.setErrorResponse` writes `error`, not `errorMessage`

`com.liferay.support.tools.portlet.actions.ResourceCommandUtil.setErrorResponse` writes the failure message to the JSON field named `error`. Do not invent alternate field names (`errorMessage`, `message`, `reason`, `detail`). The frontend `parseResponse` in `js/utils/api.ts` only reads `error`.

## 11. JSONWS paths for module-level services use a dot-prefixed context

Portal-core services use simple paths: `/api/jsonws/user/get-user-by-email-address`. Module services (OSGi JARs) require a dot-prefixed module context:

| Service | JSONWS path |
|---------|-------------|
| Blogs | `/api/jsonws/blogs.blogsentry/get-group-entries` |
| Journal | `/api/jsonws/journal.journalarticle/get-articles` |
| DDM | `/api/jsonws/ddm.ddmstructure/get-structures` |

The module name matches the `Bundle-SymbolicName` prefix (e.g. `com.liferay.blogs.service` → `blogs`). Omitting the prefix returns HTTP 404.

Note: DXP 2026.Q1.3-LTS exposes JSONWS at `/api/jsonws/` (unchanged from earlier releases). `BaseLiferaySpec.jsonwsGet/Post` centralizes this; individual specs pass only the path suffix.

## 12. `PanelCategoryKeys.CONTROL_PANEL_APPS` — `MARKETPLACE` constant does not exist in CE 7.4

The constant `PanelCategoryKeys.CONTROL_PANEL_MARKETPLACE` (`"control_panel.marketplace"`) is defined in the API JAR but no `PanelCategory` component implements it on CE 7.4 GA132. Portlets registered under it are orphaned and invisible.

In DXP 2026 the Marketplace category exists, but for this project the portlet is registered under `PanelCategoryKeys.CONTROL_PANEL_APPS` (`"control_panel.apps"`). Use `panel.app.order` lower than 100 to appear first in the Apps section. Do not reference `CONTROL_PANEL_MARKETPLACE`.

## 13. `bnd.bnd` must exclude `javax.servlet` and `javax.servlet.http`

DXP 2026 does not export `javax.servlet` or `javax.servlet.http` from the OSGi runtime. Any bundle that tries to import these packages will fail with UNSATISFIED state at activation time.

Add this to `modules/liferay-dummy-factory/bnd.bnd`:

```
Import-Package: !javax.servlet,!javax.servlet.http,*
```

Without this exclusion the bundle will show as UNSATISFIED in GoGo Shell even though the code compiles successfully.

## 14. JSONWS base path is `/api/jsonws/`

DXP 2026.Q1.3-LTS keeps the JSONWS base path at `/api/jsonws/`. Earlier migration notes speculated the path had moved to `/portal/api/jsonws/`, but `/portal/api/jsonws/*` is not registered in this release and returns 404.

`BaseLiferaySpec.jsonwsGet/Post` centralizes the base path. Individual specs and cleanup code must never hard-code the full path — pass only the suffix (e.g. `'user/get-current-user'`). When sweeping for old-path references, grep both dotted access and string literals:

```bash
grep -rn '"/api/jsonws/' integration-test/src/
grep -rn "'/api/jsonws/" integration-test/src/
```

DXP 2026 BasicAuth + SAP gotchas around JSONWS auth are detailed in `docs/details/dxp-2026-gotchas.md` §10–§15.

## 15. `BaseAuthVerifierConfiguration.urlsIncludes()` returns `String`, not `String[]`

The OCD method signature is:

```java
@AttributeDefinition(required = false)
public String urlsIncludes() default "/*";
```

`.config` files for any verifier that extends `BaseAuthVerifierConfiguration` (BasicAuth, PortalSession, OAuth2, etc.) MUST use **comma-separated String** form, NOT array form:

```
# Correct
urlsIncludes="/api/*,/o/*,/xmlrpc/*"

# Wrong — silently breaks URL matching
urlsIncludes=["/api/*","/o/*","/xmlrpc/*"]
```

The array form parses successfully and serializes as `[Ljava.lang.String;@<hash>` (Java's default `toString` for arrays). Liferay then treats that gibberish as a single URL pattern that matches nothing, and BasicAuth silently falls through to the Guest user on every request. There is no warning in the log.

The same rule applies to `urlsExcludes`, and to any `@AttributeDefinition` whose return type is `String` rather than `String[]`. When in doubt, read the OCD interface in
`/home/yasuflatland/tmp/liferay-portal/portal-impl/src/com/liferay/portal/security/auth/verifier/internal/.../BaseAuthVerifierConfiguration.java`.

## 16. SAP entries are persisted in `SAPEntry` table — `.config` cannot update existing rows

`SAPServiceVerifyProcess` runs once per company and **only creates missing rows**. It never updates existing ones. Once `SYSTEM_DEFAULT` and `SYSTEM_USER_PASSWORD` rows are persisted, an OSGi `.config` override of `SAPConfiguration` has **no effect** on those rows.

To widen an existing SAP entry at test time, mutate it in-process via `SAPEntryLocalService.updateSAPEntry(...)`. This is a local-service call and is not subject to HTTP-layer SAP enforcement. Reference: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/sap/SAPTestSetup.java`.

```java
// Widen SYSTEM_USER_PASSWORD to allow all service signatures.
SAPEntry entry = _sapEntryLocalService.fetchSAPEntry(
    companyId, "SYSTEM_USER_PASSWORD");

_sapEntryLocalService.updateSAPEntry(
    entry.getSapEntryId(), "*", entry.isDefaultSAPEntry(), true,
    entry.getName(), entry.getTitleMap(), new ServiceContext());
```

The `configuration.override.com.liferay.portal.security.service.access.policy.configuration.SAPConfiguration_systemDefaultSAPEntryServiceSignatures=*` entry in `portal-ext.properties` only affects the **default values used when creating missing rows on a fresh database** — it does not retroactively widen an existing row. For test runs against a fresh container (`removeDockerContainer` → `startDockerContainer`) the override does take effect because rows have not yet been seeded; for runs that reuse a container it does not.

## 18. `group/get-group` returns `name` as locale-aware XML; use `nameCurrentValue`

`GroupService.getGroup(groupId)` via `group/get-group` returns the `name` field as a locale-aware XML blob:

```
<?xml version="1.0" ?><root available-locales="en_US" default-locale="en_US"><Name language-id="en_US">sample-site</Name></root>
```

Asserting `name.startsWith('sample-site')` fails because the value starts with `<?xml`. Use `nameCurrentValue` (locale-resolved plain string) or `descriptiveName` instead:

```groovy
(createdSite.nameCurrentValue as String).startsWith('sample-site')
```

## 19. `group/delete-group` JSONWS returns HTTP 404 in DXP 2026

`GroupService.deleteGroup` is not exposed via `/api/jsonws/group/delete-group` in DXP 2026 — the endpoint returns HTTP 404. Cleanup code that calls it silently fails, leaving site groups behind. Downstream specs that reuse fixed site names then collide on re-create.

Workaround: use a unique per-run suffix on `baseName` for specs that share a container lifecycle so re-creates do not collide even if cleanup is skipped. Document skipped cleanup with a comment (see `CompanyFunctionalSpec` pattern in `.claude/rules/testing.md`). There is no known working JSONWS deletion path for `Group` in this release.

## 17. `SYSTEM_USER_PASSWORD` SAP requires `passwordBasedAuthentication=true` on the AuthVerifierResult

The `SYSTEM_USER_PASSWORD` SAP policy only activates when the inbound request's AuthVerifierResult carries `passwordBasedAuthentication=true`. Two verifiers set this flag:

- `PortalSessionAuthVerifier` (when the session was established by a username/password login)
- `BasicAuthHeaderAutoLoginSupport.doLogin()` (when BasicAuth credentials succeed)

`BasicAuthHeaderAutoLoginSupport.doLogin()` itself gates on `BasicAuthHeaderSupportConfiguration.enabled` (COMPANY scope). DXP 2026 ships with this disabled by default — see `docs/details/dxp-2026-gotchas.md` §11. Without enabling it, even a successful BasicAuth handshake fails to set `passwordBasedAuthentication=true`, so `SYSTEM_USER_PASSWORD` never fires and the call falls through to `SYSTEM_DEFAULT`'s narrower allowlist.

## 20. `LayoutSet.getLayoutSetPrototypeUuid()` returns `""` when no prototype is linked

`LayoutSet.getLayoutSetPrototypeUuid()` returns an empty string `""` — not `null` — when the layout set has no linked site template prototype. A `!= null` guard alone is insufficient: the empty string passes the null check and leaks into response JSON as `"publicLayoutSetPrototypeUuid": ""`.

Normalize with a null-if-empty helper before storing the value in a result record:

```java
private static String _nullIfEmpty(String value) {
    return ((value == null) || value.isEmpty()) ? null : value;
}
```

The same empty-string behavior applies to other prototype-related accessors on `LayoutSet` (e.g. `getLayoutSetPrototypeKey()`). Reference: `UserCreateUseCase._toItemResult`, `SiteCreateUseCase._toItemResult`.

## 21. `UserLocalService.addUserWithWorkflow` — new `int type` parameter at position 20

DXP 2026 adds a required `int type` argument at position 20 (between `jobTitle` and `groupIds`). This parameter did not exist in CE 7.4 or earlier. **Always pass `UserConstants.TYPE_REGULAR` (value 1) for end-user accounts.**

Passing `0` (= `UserConstants.TYPE_GUEST`) or any value other than `TYPE_REGULAR` makes the user invisible in Control Panel > Users and Organizations because that view filters on `type == 1`. The bug is silent — the user is created, but the Control Panel will not display it.

Verified signature:

```java
addUserWithWorkflow(
    long creatorUserId, long companyId,
    boolean autoPassword, String password1, String password2,
    boolean autoScreenName, String screenName, String emailAddress,
    Locale locale,
    String firstName, String middleName, String lastName,
    long prefixListTypeId, long suffixListTypeId,
    boolean male,
    int birthdayMonth, int birthdayDay, int birthdayYear,
    String jobTitle,
    int type,                           // <-- position 20, NEW in DXP 2026: use UserConstants.TYPE_REGULAR
    long[] groupIds, long[] organizationIds, long[] roleIds, long[] userGroupIds,
    boolean sendEmail,
    ServiceContext serviceContext)
```

**Gotcha for future debugging**: If this regression resurfaces (the `type` argument silently reverts to a literal `0`), `Calendar.JANUARY` (also `0`) earlier on the same line makes the regression visually invisible — when scanning the line for the offending `, 0,`, check column position, not just the digit. The original bug at this site went undetected for that reason.

Reference: `UserCreator.java`.
