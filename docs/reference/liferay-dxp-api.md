# Liferay DXP API reference

Non-obvious facts about the Liferay DXP APIs this project uses, verified against the target platform in `gradle.properties` (`liferay.workspace.target.platform.version`). Each fact lives here once; rules and guides link to it. Runtime configuration (auth, `.config` files) is in [`dxp-runtime-config.md`](dxp-runtime-config.md).

When a signature below needs re-checking, read the interface in the matching `liferay-portal` release tag (e.g. `portal-kernel/src/com/liferay/portal/kernel/service/GroupLocalService.java`).

## Dependencies and packaging

### `release.dxp.api`: never pin `version: "default"`

All Liferay APIs come from one BOM-managed artifact, declared **without a version**:

```groovy
compileOnly group: "com.liferay.portal", name: "release.dxp.api"
```

`version: "default"` looks harmless but the Liferay Gradle plugin rewrites it to `latest.release`, ignoring `liferay.workspace.product`. When Liferay published `2026.q3.3`, `default` silently jumped from `2026.q1.9` and `compileJava` broke with no change in this repository. Without a version, the workspace target-platform BOM (`release.dxp.bom.compile.only:<target.platform.version>`) pins the API, so it moves only when `gradle.properties` is bumped deliberately.

Do not add individual API artifacts next to it (journal, DDM, message boards, …): they are already in the BOM, and a second copy causes version skew (`ClassCastException`, `NoClassDefFoundError`).

### `bnd.bnd` must exclude `javax.servlet`

DXP 2026 does not export `javax.servlet` or `javax.servlet.http`. A bundle importing them compiles but stays UNSATISFIED. `bnd.bnd` must keep:

```
Import-Package: !javax.servlet,!javax.servlet.http,*
```

### Portlet API namespace

DXP 2026 uses Jakarta Portlet 4.0: `jakarta.portlet.*` imports and `jakarta.portlet.*` component properties with `jakarta.portlet.version=4.0`. The JSP taglib URI is the exception — it stays `http://xmlns.jcp.org/portlet_3_0`, because that is the only URI DXP advertises in `Provide-Capability`; `jakarta.tags.portlet` fails bundle resolution. Decision record: [ADR-0008](../adr/0008-dxp-2026-migration.md).

### Control Panel category

The portlet registers under `PanelCategoryKeys.CONTROL_PANEL_APPS` (`control_panel.apps`) with `panel.app.order` below 100 so it appears first. Do not use `CONTROL_PANEL_MARKETPLACE`.

## Service API signatures

### `GroupLocalService.addGroup` — 17 arguments

DXP 2026 added `externalReferenceCode` (first) and `typeSettings` (directly after `type`):

```java
_groupLocalService.addGroup(
	externalReferenceCode,          // null → generated
	userId, parentGroupId, className, classPK,
	liveGroupId, nameMap, descriptionMap, type,
	typeSettings,                   // null or StringPool.BLANK → defaults
	manualMembership, membershipRestriction, friendlyURL,
	site, inheritContent, active,
	serviceContext);
```

Used by `SiteCreator`.

### `UserLocalService.addUserWithWorkflow` — `int type` at position 20

DXP 2026 added a required `int type` between `jobTitle` and `groupIds`. Always pass `UserConstants.TYPE_REGULAR` (1). Any other value (e.g. `0`, `TYPE_GUEST`) creates the user but hides it from Control Panel → Users, which filters on `type == 1`.

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
	int type,                       // UserConstants.TYPE_REGULAR
	long[] groupIds, long[] organizationIds, long[] roleIds, long[] userGroupIds,
	boolean sendEmail,
	ServiceContext serviceContext)
```

If this regresses to a literal `0`, it hides well: `Calendar.JANUARY` is also `0` on the same line. Check the argument position, not just the digit. Used by `UserCreator`.

### `CompanyLocalService.addCompany` — 13 arguments only

```java
addCompany(Long companyId, String webId, String virtualHostname, String mx,
	int maxUsers, boolean active, boolean addDefaultAdminUser,
	String defaultAdminPassword, String defaultAdminScreenName,
	String defaultAdminEmailAddress, String defaultAdminFirstName,
	String defaultAdminMiddleName, String defaultAdminLastName)
```

The shorter overload lives on `CompanyService`, which is blacklisted for remote use. For dummy companies pass `addDefaultAdminUser=false` and `null` admin fields. Used by `CompanyCreator`.

**Call it with an empty `ServiceContext` on the thread.** `addCompany` runs the site initializers, and indexing the CMS site's layouts renders them through `LayoutServiceContextHelper`. That helper reuses the response of the thread's `ServiceContext` (or its theme display), and a CMS fragment calls `sendRedirect` on it, so a resource request answers HTTP 302 instead of JSON. When the thread's context has no request and no response, Liferay renders into a dummy response instead. `CompanyCreator` therefore pushes `new ServiceContext()` around each `addCompany` transaction and pops it in `finally`; `CompanyCreatorTest` locks this.

### `AssetCategoryLocalService.addCategory` — `boolean system` (2026.Q3+)

From 2026.Q3 the full overload takes `boolean system` between `vocabularyId` and `categoryProperties`; the older 9-argument form no longer compiles.

```java
addCategory(String externalReferenceCode, long userId, long groupId,
	long parentCategoryId, Map<Locale, String> titleMap,
	Map<Locale, String> descriptionMap, long vocabularyId,
	boolean system, String[] categoryProperties,
	ServiceContext serviceContext)
```

Pass `system=false` for ordinary categories. Used by `CategoryCreator`.

### `MBCategoryLocalService.addCategory` — 6-argument form

The 5-argument overload without `externalReferenceCode` is gone. Use:

```java
addCategory(String externalReferenceCode, long userId, long parentCategoryId,
	String name, String description, ServiceContext serviceContext)
```

`externalReferenceCode=null`, `parentCategoryId=0L` for a top-level category.

### `MBThreadLocalService.getThreads` — `categoryId` is an exact match

`categoryId=0L` returns only root-level threads, not every thread in the group. To list all threads, union the root call with one call per `MBCategoryLocalService.getCategories(groupId)` entry.

### `ServiceContext.setAssetTagNames` — auto-create and group scope

Setting a non-empty `String[]` before a content-creation call makes Liferay (`AssetTagLocalServiceImpl.checkTags`) attach each tag to the new `AssetEntry` and create any tag missing in the current `scopeGroupId`. Tags are group-scoped: the same name in two sites is two `AssetTag` rows. `name` is stored as written, which is why our boundary lowercases tags (`AssetTagNames`).

### `LayoutSet.getLayoutSetPrototypeUuid()` returns `""`, not `null`

Without a linked site template the prototype accessors (`getLayoutSetPrototypeUuid()`, `getLayoutSetPrototypeKey()`, …) return an empty string. A `!= null` guard lets `""` leak into JSON; normalize with a null-if-empty helper. See `UserCreateUseCase._toItemResult`.

### `DefaultScreenNameValidator` accepts only `[a-zA-Z0-9._-]`

It also rejects email-like names, purely numeric names and reserved words (e.g. `postfix`), and does not lowercase. Failure surfaces as `UserScreenNameException.MustValidate`. Datafaker names with apostrophes, spaces or non-ASCII characters fail, so faker output goes through `ScreenNameSanitizer.sanitize`, which strips illegal characters, collapses repeated `.`, trims leading/trailing `._-`, and falls back to `"user"` (logged at WARN) for `null` or empty results. The caller lowercases and appends any index suffix. User-typed names are rejected instead — see the [input boundary policy](../architecture/backend.md#input-boundary-policy).

## JSON Web Services (`/api/jsonws/`)

### What is exposed

- The base path is `/api/jsonws/`. (`/portal/api/jsonws/` does not exist and returns 404.) `BaseLiferaySpec.jsonwsGet/Post` owns the base path; specs pass only the suffix, e.g. `'user/get-current-user'`.
- Only remote `*Service` classes are exposed — never `*LocalService`.
- Services listed in `json.service.invalid.class.names` are blacklisted. `CompanyServiceUtil` is one: every `/api/jsonws/company/*` call returns 404. For the current company id, read `companyId` from `user/get-current-user`.
- Module services need a dot-prefixed context named after the bundle: `blogs.blogsentry/get-group-entries`, `journal.journalarticle/get-articles`, `ddm.ddmstructure/get-structures`. Without the prefix: 404.

Before writing verification or cleanup for a new entity, check both: does a remote service method exist, and is it blacklisted?

### Known endpoint behaviour

| Endpoint | Behaviour | Do this instead |
|---|---|---|
| `group/get-group` | `name` is locale XML (`<?xml …><root …><Name …>site</Name></root>`) | Assert on `nameCurrentValue` or `descriptiveName` |
| `group/delete-group` | 404 — not exposed | No JSONWS cleanup for groups; give per-run unique names |
| `assetentry/get-entry` | Omits derived collections such as `tagNames` (reading it NPEs) | `classname/fetch-class-name-id?value=<class>` then `assettag/get-tags?classNameId=<id>&classPK=<pk>` |
| `organization/add-organization` | Still exposed | Headless Admin User `POST /o/headless-admin-user/v1.0/organizations` is the recommended path |

## Headless REST (`/o/headless-*`)

- **`?search=` goes through Elasticsearch** and lags behind writes (seconds, longer under load). For post-create assertions fetch `?pageSize=100` and filter client-side. The `message-board-sections` listing without `search` is database-backed.
- **Message-board naming differs between layers;** ids are shared, so create with one and verify with the other:

| Java API | Headless API |
|---|---|
| `MBCategory` | `message-board-section` |
| `MBThread` | `message-board-thread` |
| `MBMessage` | `message-board-message` |
