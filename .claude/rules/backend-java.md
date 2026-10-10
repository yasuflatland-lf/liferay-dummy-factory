---
paths:
  - "modules/liferay-dummy-factory/src/main/java/**"
  - "modules/liferay-dummy-factory/bnd.bnd"
  - "modules/liferay-dummy-factory/build.gradle"
---

# Backend Java rules

Explanations live in `docs/architecture/backend.md`; Liferay API facts in `docs/reference/liferay-dxp-api.md`. Read the relevant section before changing a Creator, resource command or workflow adapter.

## Contracts (do not break)

- **Creators** return `BatchResult<T>` via `BatchResult.success(...)` / `failure(...)`, declare `throws Throwable`, and wrap each entity in `BatchTransaction.run(...)`. No local `TransactionConfig`, no direct `TransactionInvokerUtil`.
- **`success` is strict** (`count == requested`); `error` is set iff `success == false`; `requested`/`skipped` always present; the items key is `items`. A catch-and-continue loop must count `skipped`.
- **Input boundary:** reject user input (never rewrite it), sanitize generated data (`ScreenNameSanitizer`), and throw validation errors before the first transaction.
- **Resource commands** delegate to `PortletJsonCommandTemplate.serveJsonWithProgress` (except `DocumentUploadResourceCommand`), serialize with `ResourceCommandUtil.toJson`, validate ids with `validatePositiveId` (plain `< 0` only where `0` is a sentinel). Failure field name is `error` — nothing else.
- **New `/ldf/*` command** ⇒ register it in `view.jsp` (resource URL + `actionResourceURLs` entry) in the same change.
- **Workflow adapters** return `WorkflowResultNormalizer.normalize(...)`, read optional parameters with `WorkflowParameterValues.optional*`, and expose exactly the same item fields, in the same order, as the resource command (`docs/reference/workflow-api.md`). When a `*BatchSpec` changes, update both the adapter and any `*Request` helper.
- **`parseBatchSpec`** requires a `baseName` field in the matching frontend form.
- **Constants** shared between layers live in `constants/LDFPortletKeys`; the service layer never references a resource command class (e.g. use `LDFPortletKeys.DOCUMENT_TEMP_FOLDER_NAME`).
- **UseCase** only for read-only, non-fatal enrichment after the Creator (catch `Exception`, WARN with the exception, sentinel values). Never inside a Creator loop; not for mere field projection.
- **`DataListProvider`** needing request parameters overrides the 3-argument `getOptions`.

## Platform (DXP 2026)

- `jakarta.portlet.*` imports and `jakarta.portlet.version=4.0`; JSP taglib URI stays `http://xmlns.jcp.org/portlet_3_0`.
- `bnd.bnd` keeps `Import-Package: !javax.servlet,!javax.servlet.http,*`.
- `release.dxp.api` has **no** version (never `"default"`), and no per-API artifacts beside it.
- Changed Liferay service signatures (`addGroup`, `addUserWithWorkflow`, `addCategory`, …) are catalogued in `docs/reference/liferay-dxp-api.md`; check it before calling one and add to it when you find a new one.

## Java conventions

- Tabs. Private members `_prefixed`. Liferay continuation indent (wrapped parameters +2 tabs, `throws` +1 tab).
- Imports: `com.liferay.*`, then third-party, then `java.*`/`javax.*`, then `org.*`, blank line between groups. No unused imports.
- `@Component(property = {...}, service = X.class)` with one quoted property per line and `service` after the property array.
- Prefer `@Reference` services over `*Util` statics (exception: `BatchTransaction` encapsulates `TransactionInvokerUtil`).
- A `@Component` with a test constructor also declares an explicit public no-arg constructor — OSGi DS needs it (`CompanyCreateWorkflowOperationAdapter`).
- `init.jsp` includes both `<liferay-theme:defineObjects />` and `<portlet:defineObjects />`.
- No speculative abstractions, no defensive try/catch around internal code, comments only for non-obvious *why*.
- A new class under `utils/` gets a matching JUnit test in `src/test/java`.
