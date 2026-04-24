# Writing Code

L2 layer for new features, refactoring, and general code modifications. For tests use `.claude/rules/testing.md`, for PR review use `.claude/rules/code-review.md`, for bug investigation use `.claude/rules/debugging.md`.

## Project Structure

```
liferay-dummy-factory/
  modules/liferay-dummy-factory/   # Single OSGi bundle (portlet + web + React)
    src/main/java/                  # MVCPortlet, ResourceCommands, Creator services
    src/main/resources/META-INF/resources/js/   # React frontend
  integration-test/                 # Spock + Testcontainers (repo root, NOT under modules/)
```

Detailed DXP 2026 API constraints: `docs/details/api-liferay-dxp2026.md`. Workspace frontend traps: `docs/details/workspace-frontend-traps.md`. Read on demand.

## Portlet Module

- **Single-JAR design** — MVCPortlet, MVCResourceCommand, and React frontend ship together in one bundle (`liferay.dummy.factory`).
- **MVCPortlet + PanelApp** — Registered in Control Panel > Configuration. Uses `jakarta.portlet` namespace (Portlet API 4.0). DXP 2026 requires `jakarta.portlet.*` imports (see `docs/ADR/adr-0008-dxp-2026-migration.md`).
- **`LDFPortletKeys.DOCUMENT_TEMP_FOLDER_NAME`** — the temp-folder name used by `DocumentUploadResourceCommand` (add/delete temp) and `DocumentCreator` (_loadTempFiles). **Never reference `DocumentUploadResourceCommand` from the service layer**; always use the constant from `constants/LDFPortletKeys`.
- **MVCResourceCommands** — Per-entity resource commands handle creation: `/ldf/blog`, `/ldf/company`, `/ldf/org`, `/ldf/user`, `/ldf/role`, `/ldf/site`, `/ldf/page`, `/ldf/wcm`, `/ldf/doc` (+ `/ldf/doc/upload`), `/ldf/vocabulary`, `/ldf/category`, `/ldf/mb-category`, `/ldf/mb-thread`, `/ldf/mb-reply`. `/ldf/data` (`DataListResourceCommand`) serves dropdown data; `/ldf/progress` (`ProgressResourceCommand`) reports batch progress.
- **Value Objects** — `BatchSpec` (Java record) encapsulates `count + baseName` with constructor validation. `EmailDomain` (Java record) wraps a domain string, rejects blanks and `@`-containing inputs, and defaults to `"liferay.com"` via `EmailDomain.of(raw)`. `RoleType` and `SiteMembershipType` are type-safe enums mapping frontend strings to Liferay constants. `BatchResult<T>` (generic record) is the **unified Creator return type** — see the "Creator return type is always `BatchResult<T>`" section below. Resource commands construct value objects from JSON before passing to Creators.
- **DataListProvider SPI** — Dropdown sources are `DataListProvider` implementations discovered via OSGi `@Reference(cardinality=MULTIPLE, policy=DYNAMIC)`. Add a new type by creating `@Component(service=DataListProvider.class)` under `service/datalist/` — no changes to `DataListResourceCommand` needed.
- **`bnd.bnd` must exclude `javax.servlet`**: DXP 2026 does not export `javax.servlet` or
  `javax.servlet.http` from the OSGi runtime. Always include this line in
  `modules/liferay-dummy-factory/bnd.bnd`:
  ```
  Import-Package: !javax.servlet,!javax.servlet.http,*
  ```
  Without it, the bundle will show as UNSATISFIED at activation time.

## Java Conventions

- **Tab indentation** (no spaces).
- Prefer `@Reference` injection over `*Util` static classes (`TransactionInvoker` over `TransactionInvokerUtil`, `RoleLocalService` over `RoleLocalServiceUtil`) for testability.
- Private fields/methods get an underscore prefix: `_privateField`, `_doSomething(...)`.
- `@Component` annotations use array-style `property = { ... }` with one quoted string per line. The `service` attribute lives on its own line after the closing brace.
- Use `jakarta.portlet` imports. DXP 2026 requires `jakarta.portlet.*` imports and
	`jakarta.portlet.version=4.0` in `@Component` property arrays. Do NOT use `javax.portlet.*`.
	JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0` — the JCP namespace is what
	DXP 2026 advertises via `Provide-Capability`. Switching to `jakarta.tags.portlet` in JSPs
	causes bundle resolution failure. See `docs/ADR/adr-0008-dxp-2026-migration.md`.
- Import order: `com.liferay.*` → third-party → `javax.*`/`java.*` → `org.*`. Blank line between groups.
- Multi-line method parameters use Liferay's continuation indent: second line +2 tabs, `throws` clause +1 tab.
- `init.jsp` must include both `<liferay-theme:defineObjects />` and `<portlet:defineObjects />`.
- **`init.jsp` taglib URI must stay JCP**: Use `http://xmlns.jcp.org/portlet_3_0` as the
  portlet taglib URI in all JSPs. DXP 2026's `Provide-Capability` only advertises this URI.
  Switching to `jakarta.tags.portlet` causes bundle resolution failure. A comment in
  `init.jsp` marks this as intentional — do not change it.
- **OSGi DS `@Component` constructor pattern** — Adding any explicit constructor to a `@Component` class that uses `@Reference` field injection eliminates the JVM's implicit no-arg constructor. OSGi DS instantiates components via the no-arg constructor, so the component fails to activate if it is absent. Always declare **both** constructors: `public ClassName() {}` (OSGi activation) and `package-private ClassName(Dep dep, ...)` (unit-test injection). Reference: `CompanyCreateWorkflowOperationAdapter.java`.

## Creator pattern (`BatchTransaction.run` + `throws Throwable`)

Every `*Creator` under `service/` wraps each per-entity call in `com.liferay.support.tools.utils.BatchTransaction.run(() -> { ... })`:

- `BatchTransaction.run` centralizes the transaction config (`Propagation.REQUIRED`, rollback on `Exception.class`) so future cross-cutting changes (retry, metrics, MDC) apply in one place instead of 12+ Creators. Do NOT declare a local `TransactionConfig` field in a Creator and do NOT inline `TransactionInvokerUtil.invoke(_transactionConfig, ...)`.
- Method signature declares `throws Throwable` because `BatchTransaction.run` itself throws `Throwable`.
- Corresponding `*ResourceCommand` catches `Throwable` (NOT `Exception`) in `doServeResource` and routes to `ResourceCommandUtil.setErrorResponse`.

**Why**: a mid-loop failure must not roll back already-created entities; each per-entity transaction commits independently.

Reference: `VocabularyCreator.java`, `OrganizationCreator.java`.

### Usecase-layer enrichment: non-fatal try-catch

When a usecase class performs secondary enrichment after a Creator call (e.g. fetching `LayoutSet` UUIDs to build a richer DTO), that enrichment must not abort the batch if it fails — the Creator's entities are already committed. Wrap enrichment-only calls in `try-catch (Exception e)`, log at WARN with the exception object, and return sentinel values (`null`, `0L`) for the optional fields.

This pattern applies **only** to read-only secondary lookups in the usecase layer. It does NOT apply inside a Creator's per-entity loop, which uses `BatchTransaction.run` with independent transactions. Reference: `UserCreateUseCase._toItemResult`, `SiteCreateUseCase._toItemResult`.

**When NOT to introduce a UseCase layer**: If the Creator already returns a Liferay model object (`AssetCategory`, `AssetVocabulary`, `MBCategory`, `MBMessage`, etc.) that exposes all fields needed in the response via getters, add those fields directly to the RC's `toJson` lambda — no UseCase class is needed. A UseCase layer is only justified when a secondary service call with non-fatal try-catch is required (e.g. fetching `LayoutSet` UUIDs). Introducing UseCase purely to project model fields is over-engineering.

## ResourceCommand pattern (`PortletJsonCommandTemplate`)

Every `*ResourceCommand` (except `DocumentUploadResourceCommand` which handles file uploads) must delegate to `PortletJsonCommandTemplate.serveJsonWithProgress()`:

```java
PortletJsonCommandTemplate.serveJsonWithProgress(
    resourceRequest, resourceResponse, _portal, _log,
    "Failed to create <entities>",
    (context, data, responseJson) -> {
        // parse params from data, validate, call Creator
        return ResourceCommandUtil.toJson(result, item -> { ... });
    });
```

The template handles: `HttpServletRequest`/`data` parsing, `ProgressManager` lifecycle, `IllegalArgumentException` and `Throwable` catch routing via `ResourceCommandUtil.setErrorResponse`, and `JSONPortletResponseUtil.writeJSON`. Do NOT replicate these in a new ResourceCommand.

**`PortletJsonCommandContext` provides:**
- `context.getUserId()` — current user ID
- `context.getCompanyId()` — current company ID (throws `IllegalArgumentException` if unresolvable)
- `context.getProgressCallback()` — progress reporter wired to `ProgressManager`

**`_log` and `_portal` fields must remain in each ResourceCommand** — they are passed to the template as arguments.

**WCM exception**: `WcmResourceCommand` returns a custom JSON object (`ok`, `totalRequested`, `totalCreated`, `perSite`) from its lambda instead of using `ResourceCommandUtil.toJson()`. Even custom JSON objects **must emit `"error"` when `result.success() == false`**:

```java
if (!result.success()) {
    json.put("error", result.error());
}
```

**`ResourceCommandUtil` validation helpers:**
- `validateCount(int count)` — rejects count ≤ 0
- `validatePositiveId(long id, String fieldName)` — rejects id ≤ 0
- `validateNotEmpty(String value, String fieldName)` — rejects null or blank strings

Use `validatePositiveId` for every `id > 0` constraint instead of a hand-written `if (id <= 0)` block — the helper keeps error messages consistent. The one exception is when `0` is a valid sentinel (e.g. `categoryId = 0` means "root MBCategory") — use `if (id < 0)` explicitly in that case.

## DXP 2026 API call shapes

### `GroupLocalService.addGroup` — 18-argument signature

DXP 2026 adds `externalReferenceCode` as the first argument and `typeSettings`
before `serviceContext`. Pass `null` for both new parameters when no custom value is needed:

```java
_groupLocalService.addGroup(
	null,                // externalReferenceCode (auto-generated)
	userId, parentGroupId, className, classPK,
	liveGroupId, nameMap, descriptionMap, type,
	manualMembership, membershipRestriction, friendlyURL,
	site, inheritContent, active,
	null,                // typeSettings (defaults)
	serviceContext);
```

Full API constraints: `docs/details/api-liferay-dxp2026.md`.

### Early validation outside the transaction boundary

When a Creator validates input before looping (e.g. a regex check on `baseName`), throw the validation exception **outside** any `TransactionInvokerUtil.invoke(...)` call. No transaction has started, so no rollback is needed. The `throws Throwable` contract plus the resource command's `catch (Throwable)` automatically routes the exception through `ResourceCommandUtil.setErrorResponse` → `{success: false, error: "..."}`. No extra plumbing required.

Do NOT wrap validation in `invoke(...)` "for consistency" with per-entity calls — it costs a meaningless null-rollback and hides the contract that input validation happens at the boundary.

- **Response key for batch items is always `items`** — Every Creator's response JSON uses the key `items` for the array of created entities, regardless of entity type. Do NOT invent entity-specific keys like `"users"`, `"roles"`, `"organizations"`. The full response contract is `{success, requested, count, skipped, items, error?}` — see the "Batch Creator response contract" bullet for invariants.
- **`WebContentCreator` items are per-site, not per-article** — `WebContentCreator` batches across multiple sites (`groupIds`), so its `BatchResult<WebContentPerSiteResult>` carries one item per site (`{groupId, siteName, created, failed, error}`). Flattening to one item per article would lose per-site failure attribution — the UI could no longer display "Site A succeeded, Site B failed". Because `count` is total articles created (not items.size()), `WebContentCreator` uses the `BatchResult` canonical constructor directly rather than the `success()`/`failure()` factories. `WebContentPerSiteResult` itself enforces its own invariant: `failed > 0` requires a non-blank `error`, and `error` requires `failed > 0` — this mirrors `BatchResult`'s success/error invariant at the site level.
- **Typed `*BatchSpec` records absorb per-batch parameters** — When a Creator's method signature grows past ~5 parameters, extract the per-batch configuration into a Java `record` named `*BatchSpec` (e.g. `UserBatchSpec`, `WebContentBatchSpec`). The record composes the shared `BatchSpec(count, baseName)` via a `BatchSpec batch` field rather than inlining count/baseName. The record's compact constructor normalizes nullable/empty inputs to their documented defaults so the `*ResourceCommand` does not need to repeat defensive null checks. Reference implementations: `UserBatchSpec.java`, `WebContentBatchSpec.java`, `MBReplyBatchSpec.java`. Creators that currently have ≤5 parameters (e.g. `OrganizationCreator`, `RoleCreator`) should continue to use raw parameters + `BatchSpec` — do not introduce a dedicated spec where it is not needed.
- **`parseBatchSpec` requires a `baseName` field in the frontend form** — `ResourceCommandUtil.parseBatchSpec(data)` reads both `count` and `baseName` from the JSON payload and passes them to `new BatchSpec(...)`. `BatchSpec`'s compact constructor rejects a null or blank `baseName`. Any entity whose ResourceCommand calls `parseBatchSpec` MUST include `createBaseNameField(...)` in its `EntityFormConfig.fields` array in `entities.ts`. Omitting it causes a server-side `IllegalArgumentException` on every submission.

### Creator return type is always `BatchResult<T>`

Every batch Creator's `create(...)` method returns `BatchResult<T>`. There is no Pattern A (`JSONObject`) or Pattern B (`List<T>`) split any more — the historical multi-pattern taxonomy has been unified. Adapter code that built a `JSONObject` contract inside the Creator, or recomputed `count`/`skipped` in the ResourceCommand, should be deleted rather than ported.

`BatchResult<T>` record:

- Fields: `boolean success, int count, int requested, int skipped, List<T> items, String error`.
- Canonical constructor enforces four invariants: `requested > 0`; `count` and `skipped` are `>= 0`; `success == false` requires a non-blank `error`; `success == true` requires `count == requested`. `items` is defensively copied (`List.copyOf`).
- Factory methods: `BatchResult.success(requested, items, skipped)` and `BatchResult.failure(requested, items, skipped, error)`. Use the factories in ordinary Creators — they derive `count` from `items.size()` and make the success/failure branch self-documenting.
- Use the canonical constructor directly **only** when `count` is not `items.size()`. The one current case is `WebContentCreator._buildBatchResult`: `items` is a `List<WebContentPerSiteResult>` (one per site), but `count` is the total number of articles created across all sites. The invariant `success == true → count == requested` is what locks this.

ResourceCommand → JSON is a one-liner:

```java
JSONObject json = ResourceCommandUtil.toJson(result, item -> item.toJSONObject());
```

`ResourceCommandUtil.toJson(BatchResult<T>, Function<T, JSONObject>)` is the single entry point for assembling the `{success, count, requested, skipped, items, error?}` payload. Do not build the JSON manually in a ResourceCommand; do not recompute `count` or `skipped` from the items list. The helper writes `error` only when `success == false`, so a success result does not leak a stray `"error": null` field.

Workflow adapter → `WorkflowStepResult` is also a one-liner:

```java
return WorkflowResultNormalizer.normalize(result, item -> Map.of("id", item.getPrimaryKey(), ...));
```

`WorkflowResultNormalizer` is in `workflow.adapter.core` and is intentionally `public` so that taxonomy, message-boards, and content adapters in sibling packages can call it. It is the workflow-side analogue of `ResourceCommandUtil.toJson` — adapters should not recompute `success`/`count`/`skipped` themselves.

## Input-boundary policy: reject user input, sanitize external data

- **User-supplied strings** (e.g. `baseName` from a portlet form) must be validated and rejected at the resource-command or Creator boundary. **Never silently rewrite them** — a user typing `山田` who silently gets users named `1, 2, 3` has no way to discover the substitution.
- **External-generated data** (Datafaker, RNGs, third-party APIs) must instead be **sanitized** because the caller can't control the content.
- **Enumerated user input with safe external fallback** is a third, narrow category. When a user picks from a closed set (e.g. a `locale` dropdown with `en-US`, `ja-JP`, `zh-CN`) and the value is then handed to an external generator that may not support every member, a silent degrade to the generator's default is acceptable because the user's intent is preserved at the category level and the fallback is deterministic. This is NOT a licence to silently rewrite free-text input — the form of the user's input must still map onto the enumerated set at the boundary.
- Mixing the strategies produces silent UX bugs (sanitizing user input) or probabilistic test failures (validating faker output).
- **Template JSON values are user input from the Creator's perspective.** Workflow sample templates in `workflowJsonWorkspace.ts` reach `UserCreator` through the same execution path as a portlet form submission (`WorkflowEngine` → `*WorkflowOperationAdapter`). A `baseName` value shipped in a sample template must therefore satisfy the same `/^[a-z0-9._-]+$/` regex that `UserCreator` enforces. Human-readable strings like `"Sample Workflow User"` (spaces, uppercase) are a shipping bug — use kebab-case (`"sample-workflow-user"`).
- **`AssetTagNames` normalizes, never rejects** — unlike `EmailDomain` / `UserCreator#baseName` (which reject at the boundary), `AssetTagNames.of(String)` is the single parse/normalize/dedup point: it lowercases, trims, splits on commas, removes empty tokens, and deduplicates via `LinkedHashSet` (preserving first-seen order). Blank or null input produces `AssetTagNames.EMPTY`. The four Creators that attach tags (WebContent, Document, MBThread, MBReply) call `ServiceContext.setAssetTagNames(tags.toArray())` only when `!tags.isEmpty()`, preserving byte-identical behavior for submissions without tags. `*BatchSpec` records that carry tags normalize `null → AssetTagNames.EMPTY` in their compact constructors so the ResourceCommand boundary needs no defensive null checks. See `docs/details/api-liferay-dxp2026.md` §22 for the Liferay-side `setAssetTagNames` / group-scope behavior.

Reference: `com.liferay.support.tools.utils.ScreenNameSanitizer` is the sanitization side; `UserCreator`'s pre-loop regex check is the rejection side.

## Batch Creator response contract

Every batch-producing `*Creator` returns:

```
{success, count, requested, skipped, error?, items}
```

- `success` is **strict**: `created == requested`, NOT `created > 0`. Requesting 10 and producing 3 is a **failure**, not a partial success. Enforced by `BatchResult`'s canonical constructor.
- Whenever `success == false`, `error` MUST be set. Enforced by `BatchResult`'s canonical constructor — constructing `new BatchResult<>(false, ..., null)` throws `IllegalArgumentException`. Do not try to bypass this with a blank string; the constructor rejects that too.
- `requested` and `skipped` are load-bearing for partial-failure diagnostics — keep them even if the current frontend doesn't consume them.

### `skipped` is first-class in the Creator, not reconstructed downstream

Because every Creator returns `BatchResult<T>`, catch-and-continue loops can track `skipped` directly and the ResourceCommand/adapter never has to guess. Do not add a catch-and-continue path to a Creator without incrementing a local `skipped` counter and passing it to `BatchResult.failure(requested, items, skipped, error)` — otherwise `count + skipped < requested` and the diagnostic is lost.

## `ResourceCommandUtil.setErrorResponse` writes `error` (not `errorMessage`)

The helper writes the failure message to the JSON field `error`. New resource commands must use this helper. Do NOT invent alternate field names like `errorMessage`, `message`, `reason`, or `detail` — the frontend `parseResponse` does not read them.

## Groovy (Integration Tests)

- Spock spec method names use the descriptive form: `def 'description of behavior'()`.
- Use `given:` / `when:` / `then:` / `expect:` blocks. Prefer `expect:` for single-expression assertions.
- Idioms: safe-navigation `?.`, `withCloseable { ... }`, GString `"${variable}"`.
- All test classes live under `com.liferay.support.tools.it.*`.
- `@Shared` for fields shared across feature methods. `@Stepwise` when ordering matters.
- Underscores in numeric literals: `30_000`, `10_000`.

## JavaScript / React

- **No direct React import** — the new JSX transform is enabled, so `import React from 'react'` is omitted. Import only the hooks you need: `import {useState} from 'react'`.
- Use **Clay CSS** utility classes (`container-fluid`, `sheet`, `form-group`, `form-control`, `btn btn-primary`, `alert alert-success`, `alert alert-danger`, …).
- Localization: always use `Liferay.Language.get('key-name')`. Never hard-code display text.
- CSRF: any `fetch` to a Liferay `/o/<app>/...` JAX-RS path MUST set both `credentials: 'include'` and `headers: {'x-csrf-token': Liferay.authToken}` — session cookie alone is rejected with HTTP 401 by PortalRealm. Applies to GET, not just POST. See J36 in `docs/details/workspace-frontend-traps.md`.
- API calls use `portlet:resourceURL` with `credentials: 'include'`. GET passes parameters as URL query strings; POST uses `application/x-www-form-urlencoded` with JSON in a `data` parameter.
- Build tooling is a custom esbuild script (`scripts/build.mjs`). `@liferay/npm-scripts` is retained for code formatting only (`format` / `checkFormat`). See `docs/ADR/adr-0003-custom-esbuild-over-npm-scripts.md`.

### `parseResponse` must check both `data.success === false` and `data.error`

`success` and `error` are **independent** fields in the backend response contract. Checking only `data.error` silently classifies `{success: false}` as success. Checking only `data.success` misses legacy responses that set only `error`. The correct form is `if (data.success === false || data.error)`.

A failure-classified response must pass the **full `data` payload** through to the caller (not just `{error, success: false}`) so partial-batch responses like `{success: false, count: 3, requested: 5, users: [...], error: "..."}` can be rendered without a second round-trip.

`ApiResponse<T>` failure variant carries optional data: `{success: false; data?: T; error: string}`.

The backend/frontend error field name is `error` — not `errorMessage`, `message`, or `reason`.

### Liferay.Language fallback gotcha

`Liferay.Language.get('missing-key')` returns the **key string itself** when not present in `Language.properties`. No warning, no console error — the raw kebab-case key is rendered to the user.

**Authoring rule**: when writing a Playwright assertion on localized text, assert on the **resolved English phrase** from `Language.properties` (e.g. `"Execution completed successfully."`), never on the key identifier. If your assertion string contains hyphens and matches the key name, it's a code smell — look up the actual value.

**Adding a key**: always add the entry to `Language.properties` in the same commit that introduces the `Liferay.Language.get('...')` call.

### Frontend i18n loading: JSP-injected ResourceBundle

**Why:** Custom ESM builds (esbuild/Vite via `scripts/build.mjs`) bypass Liferay's `LanguageUtil.process()` server-side JS replacement. Standard `@liferay/npm-bundler` portlets get `Liferay.Language.get('key')` calls rewritten to literal values at serve-time by `BuiltInJSModuleServlet`. Custom ESM bundles are served as static resources via the OSGi HTTP Whiteboard — no rewriting occurs, so `Liferay.Language._cache` starts empty and every `get()` call returns the raw key.

**What:** `view.jsp` injects the portlet's own ResourceBundle into `Liferay.Language._cache` before `<react:component>` renders. Use `portletConfig.getResourceBundle(locale)` — NOT `LanguageUtil.get(Locale, key)`, which only checks portal-global bundles and misses module-specific keys.

```jsp
<%
ResourceBundle resourceBundle = portletConfig.getResourceBundle(locale);
JSONObject languageKeys = JSONFactoryUtil.createJSONObject();
Enumeration<String> enumeration = resourceBundle.getKeys();
while (enumeration.hasMoreElements()) {
	String key = enumeration.nextElement();
	languageKeys.put(key, resourceBundle.getString(key));
}
%>
<script>
	Object.assign(Liferay.Language._cache, <%= languageKeys.toJSONString() %>);
</script>
```

### Playwright selector strategy

- Priority: `getByRole` → `aria-label` (i18n-stable only) → `data-testid`.
- **Never select by visible text** (`has-text`, `getByText`) — `Liferay.Language.get(...)` values change per locale.

### `data-testid` naming and generation contract

- **Naming**: kebab-case domain term + role (`organization-create-submit`, `user-count-input`, `role-type-select`). No UI-positional names (`btn1`) or BEM-style (`Form__submit`).
- **Placement scope**: only on elements Playwright actually interacts with (button, input, select, result/alert region, tab). Do not decorate links, icons, or purely visual elements.
- **Reusable components**: `FormField`, `DynamicSelect`, `ResultAlert` accept an optional `testId?: string` and emit `data-testid={testId}` only when provided.
- **`entityKey` derivation**: `EntityForm.tsx` computes `entityKey = config.entityType.toLowerCase().replace(/_/g, '-')`. So `ORG` → `org`, `MB_THREAD` → `mb-thread`, `USERS` → `users`. Playwright specs MUST use the derived value — do not invent synonyms like `organization-submit`.
- **`EntitySelector` tabs are NOT kebab-cased** — tabs emit `data-testid={`entity-selector-${entityType}`}` using the raw enum value (`entity-selector-MB_THREAD`). This is an intentional exception. Use upper-snake on the selector and kebab everywhere else.
- **Generation contract**: form ids are assembled mechanically as `${entityKey}-${kebab(field.name)}-${typeSuffix}`. `typeSuffix`: `text`/`number` → `input`, `select`/`multiselect` → `select`, `textarea` → `textarea`, `file` → `file`, `checkbox` → `toggle`. Submit is always `${entityKey}-submit`, result alert always `${entityKey}-result`. Adding `maxUsers: number` to `ORG` yields `org-max-users-input` predictably.

### Form-field dependency mechanisms — three, and only three

`FieldDefinition` offers three orthogonal ways for one field to react to another. Each has a distinct purpose; do not invent a fourth.

- **`dependsOn: {field, paramName}`** — *data* dependency. The field's option list is fetched from `/ldf/data` using the parent field's value as a query parameter (e.g. categories filtered by the selected site). `DynamicSelect` suspends rendering until the parent has a value.
- **`visibleWhen: {field, value}`** — *existence* dependency. The field is omitted from the DOM when the control value does not match. Use when the field is irrelevant in other states (e.g. a structure-id input that only applies to `createContentsType === '2'`).
- **`disabledWhen: {field, value}`** — *interactivity* dependency. The field stays in the DOM but is disabled when the control value matches. Use when both sides are meaningful fields that become mutually exclusive (e.g. `fakerEnable` disables `body`; `body` disables `fakerEnable`).

Pick one per constraint. Do not combine `visibleWhen` with `disabledWhen` on the same trigger — if the field has no meaning in a state, hide it; if it has meaning but shouldn't be edited, disable it.

### Toggle values are strings in form state

`FormField.tsx` coerces checkbox state via `onChange(field.name, String(e.target.checked))`, so `formData.fakerEnable` is `'true'` or `'false'` — never a boolean. `isFieldDisabled` normalizes both sides with `String(...)` so configs can write `disabledWhen: {field: 'fakerEnable', value: true}` for readability, but the runtime comparison is always string. Authors of new `disabledWhen` / `visibleWhen` entries driven by a toggle should remember that any custom comparator they add has to survive the `String(true) === 'true'` round-trip.

## DataListProvider — request-aware overload

`DataListProvider` exposes two `getOptions` overloads: 2-arg `(companyId, type)` and 3-arg `(companyId, type, HttpServletRequest)`. The default 3-arg implementation delegates to the 2-arg one and **drops the request**. If your provider needs request parameters (e.g. `groupId` for site-scoped data) you MUST override the 3-arg overload directly. Reference: `VocabulariesDataListProvider.java`, `MBCategoriesDataListProvider.java`.

## General

- No unnecessary comments. Code is self-explanatory.
- No unused imports.
- Prefer simplicity. No over-engineering or speculative abstractions.
- After any code change, verify all related documentation is consistent: `CLAUDE.md`, `README.md`, `.claude/rules/`, `docs/details/`, `docs/ADR/`, `gradle.properties`. Naming, paths, and version numbers must match the actual code.

## One package manager per repo

Never let `package-lock.json` and `yarn.lock` coexist. This project uses `yarn.lock`. Resolve a `yarn.lock` merge conflict by taking one side wholesale (`--ours` or `--theirs`) and running `yarn install` to let Yarn rewrite the file. Never hand-edit `yarn.lock`.

Dependency version pinning for the JS/React toolchain (vite/vitest/plugin-react/jsdom, `@types/react` lockstep, etc.) lives in `docs/details/dependency-policy.md`.
