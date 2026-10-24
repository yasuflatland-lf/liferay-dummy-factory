# Frontend (React, TypeScript, JSP, i18n)

Scope: `modules/liferay-dummy-factory/src/main/resources/` — `META-INF/resources/` (JSP, `js/`, `css/`) and `content/Language.properties`. Also obey [`../../../AGENTS.md`](../../../AGENTS.md) (module) and the root [`AGENTS.md`](../../../../../AGENTS.md).

Explanations live in [`docs/architecture/frontend.md`](../../../../../docs/architecture/frontend.md); the test-id contract in [`docs/reference/test-ids.md`](../../../../../docs/reference/test-ids.md). Unit-test rules for this code are in [`test/AGENTS.md`](../../../test/AGENTS.md).

## React / TypeScript

- No `import React`; import only the hooks you use. Clay CSS classes for layout and state (`sheet`, `form-group`, `btn btn-primary`, `alert alert-success`, …).
- All display text via `Liferay.Language.get(key)`; add the key to `content/Language.properties` in the same commit. Prefer passing keys through variables.
- Resource-URL calls use `credentials: 'include'`. Every `fetch` to `/o/<app>/…` also sends `headers: {'x-csrf-token': Liferay.authToken}` (or `?p_auth=`), and a Vitest test locks both.
- For 2xx responses, the `parseResponse` failure check is `data.success === false || data.error`, and a failure keeps the full payload. The error field is `error`; non-2xx handling is in [`docs/architecture/frontend.md`](../../../../../docs/architecture/frontend.md).
- Form field reactions use only `dependsOn`, `visibleWhen` or `disabledWhen` — one per constraint. Toggle values are the strings `'true'`/`'false'`.
- The workflow editor never adds client-only required fields; the schema comes from `GET /o/ldf-workflow/schema`. Keep `types/index.ts` `WorkflowRequestPayload` in step with `WorkflowResource._schemaDocument()`.
- `data-testid` values follow the generated contract; reusable components take an optional `testId` prop. Do not invent ids.

## JSP

- `init.jsp` includes both `<liferay-theme:defineObjects />` and `<portlet:defineObjects />`.
- The portlet taglib URI stays `http://xmlns.jcp.org/portlet_3_0` even though the API is `jakarta.portlet` 4.0.
