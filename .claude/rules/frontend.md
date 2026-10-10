---
paths:
  - "modules/liferay-dummy-factory/src/main/resources/**"
  - "modules/liferay-dummy-factory/test/**"
  - "modules/liferay-dummy-factory/scripts/**"
  - "modules/liferay-dummy-factory/*.{json,js,mts}"
---

# Frontend rules

Explanations live in `docs/architecture/frontend.md`; Vitest specifics in `docs/reference/vitest.md`; the test-id contract in `docs/reference/test-ids.md`.

## React / TypeScript

- No `import React`; import only the hooks you use. Clay CSS classes for layout and state (`sheet`, `form-group`, `btn btn-primary`, `alert alert-success`, …).
- All display text via `Liferay.Language.get(key)`; add the key to `content/Language.properties` in the same commit. Prefer passing keys through variables.
- Resource-URL calls use `credentials: 'include'`. Every `fetch` to `/o/<app>/…` also sends `headers: {'x-csrf-token': Liferay.authToken}` (or `?p_auth=`), and a Vitest test locks both.
- For 2xx responses, the `parseResponse` failure check is `data.success === false || data.error`, and a failure keeps the full payload. The error field is `error`; non-2xx handling is in `docs/architecture/frontend.md`.
- Form field reactions use only `dependsOn`, `visibleWhen` or `disabledWhen` — one per constraint. Toggle values are the strings `'true'`/`'false'`.
- The workflow editor never adds client-only required fields; the schema comes from `GET /o/ldf-workflow/schema`. Keep `types/index.ts` `WorkflowRequestPayload` in step with `WorkflowResource._schemaDocument()`.
- `data-testid` values follow the generated contract; reusable components take an optional `testId` prop. Do not invent ids.
- Production build is `modules/liferay-dummy-factory/scripts/build.mjs` (esbuild); do not move it to Vite or `@liferay/npm-scripts`.

## Unit tests (Vitest)

- Import test APIs from `'vitest'` (`globals: false`). Helpers stay in the spec file; only `test/setup.ts` is shared.
- Pair every localized-string assertion with the fallback guard: `expect(text).not.toBe(key)`.
- Mock hooks with the minimal-shape pattern (`as unknown as ReturnType<typeof hook>`), `Mock<FnType>` with a single function type, shared mock state via `vi.hoisted`.
- JSON shared with Spock specs lives in one fixture under `integration-test/src/test/resources/`, locked by a deep-equal parity test.

## Dependencies

- Yarn only; exact versions for the Node-sensitive toolchain; respect the lockstep pairs in `docs/reference/dependencies.md`.
- After a toolchain change, verify with a **clean** integration run (`docs/reference/test-harness.md`, incremental build trap).
