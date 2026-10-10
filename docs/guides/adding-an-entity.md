# Adding an entity type

The end-to-end checklist for a new kind of dummy data (here: a hypothetical `Widget`). Each step links to the contract it must satisfy. Work through it in order; the integration test at the end proves the wiring.

## 1. Creator (`service/`)

- [ ] `WidgetCreator` as an OSGi `@Component`, with Liferay services injected through `@Reference`.
- [ ] `public BatchResult<Widget> create(...) throws Throwable`.
- [ ] Validate user input **before** the loop, outside any transaction ([input boundary policy](../architecture/backend.md#input-boundary-policy)).
- [ ] Wrap each entity in `BatchTransaction.run(() -> …)`; never a local `TransactionConfig`.
- [ ] Return `BatchResult.success(...)` / `BatchResult.failure(...)`; if you catch and continue, count `skipped` ([batch contract](../architecture/backend.md#batch-response-contract)).
- [ ] More than ~5 parameters → a `WidgetBatchSpec` record composing `BatchSpec` ([`*BatchSpec`](../architecture/backend.md#parameters-batchspec-and-batchspec)).
- [ ] Check every Liferay call against [liferay-dxp-api.md](../reference/liferay-dxp-api.md); record any new surprise there.

## 2. Resource command (`portlet/actions/`)

- [ ] `WidgetResourceCommand` with `mvc.command.name=/ldf/widget`, delegating to `PortletJsonCommandTemplate.serveJsonWithProgress` ([pattern](../architecture/backend.md#resource-commands)).
- [ ] Parse with `ResourceCommandUtil.parseBatchSpec` and the `validate*` helpers; return `ResourceCommandUtil.toJson(result, …)`.
- [ ] Register it in `view.jsp`: a `<portlet:resourceURL id="/ldf/widget" var="widgetResourceURL" />` **and** a `.put("/ldf/widget", widgetResourceURL)` in `actionResourceURLs`. Without both, the UI and `LdfResourceClient` fail with "Could not find resource URL".

## 3. Frontend (`META-INF/resources/js/`)

- [ ] Add `WIDGET` to `ENTITY_TYPES` in `config/constants.ts`.
- [ ] Add an `EntityFormConfig` in `config/entities.ts` with `actionURL: '/ldf/widget'`. Include `createBaseNameField(...)` if the command uses `parseBatchSpec`, or every submission fails server-side.
- [ ] Field dependencies use only `dependsOn` / `visibleWhen` / `disabledWhen` ([rules](../architecture/frontend.md#form-field-dependencies)).
- [ ] Every new label key in `content/Language.properties`, in the same commit.
- [ ] Generated `data-testid`s follow automatically ([contract](../reference/test-ids.md)); use them as-is in specs.
- [ ] Dropdowns that need new data: a new `DataListProvider` under `service/datalist/` ([how](../architecture/backend.md#data-list-providers)).

## 4. Workflow operation (`workflow/adapter/<area>/`)

- [ ] A descriptor for `widget.create` in `WorkflowFunctionDescriptors` (parameters, types, required flags, descriptions, defaults).
- [ ] `WidgetCreateWorkflowOperationAdapter` (`service = WorkflowOperationAdapter.class`) building the same spec as the resource command and returning `WorkflowResultNormalizer.normalize(...)`.
- [ ] Optional parameters via `WorkflowParameterValues.optional*`.
- [ ] An operationId for `widget.create` in `WorkflowOpenAPIDocumentBuilder`'s fixed table; `WorkflowOperationIdCoverageTest` fails otherwise.
- [ ] Item fields identical, in the same order, to the resource command's `toJson` ([parity](../reference/workflow-api.md#rc--workflow-adapter-field-parity)).

## 5. Tests

- [ ] JUnit for any new pure utility or value object (`src/test/java`, mandatory for `utils/`).
- [ ] A `WidgetFunctionalSpec` extending `BaseLiferaySpec`: create through the resource command or workflow, verify through **JSONWS**, cover both branches of every new `if/else`, lock the response shape ([rules](../../.claude/rules/tests.md)).
- [ ] Vitest for new components or config logic, with the i18n guard on localized strings.
- [ ] Optional: a sample workflow under `integration-test/src/test/resources/workflow-samples/`, its TS copy, and the parity test.

## 6. Documentation

- [ ] Operations, parity and [per-operation tool](../reference/workflow-api.md#per-operation-tools) tables in [workflow-api.md](../reference/workflow-api.md).
- [ ] The entity table in [installation.md](installation.md#4-create-data) and the feature list in the [README](../../README.md).
- [ ] `node scripts/check-docs.mjs` passes.
