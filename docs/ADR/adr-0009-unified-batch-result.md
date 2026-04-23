# ADR-0009: Unify all batch Creators on `BatchResult<T>`

## Status

Accepted

## Date

2026-04-22

## Context

Previously, batch Creators returned one of three different shapes:

- **Pattern A** — Creator builds a `JSONObject` contract and returns it. The ResourceCommand forwards it. Used by `UserCreator`, `OrganizationCreator`, `RoleCreator`, `BlogsCreator`, and others. (`BlogsCreator` was the last Pattern A holdout; it has since been migrated to `BatchResult<T>`.)
- **Pattern B** — Creator returns `List<LiferayModel>`. The ResourceCommand recomputes the contract: `count = list.size()`, `success = (count == requested)`, and `skipped = 0` hard-coded because the list carries no skipped count. Used by `CategoryCreator`, `CompanyCreator`, `MBCategoryCreator`, `MBThreadCreator`, `VocabularyCreator`.
- **Pattern B+** — Creator returns a typed `BatchResult<T>` record. The ResourceCommand reads fields directly. Only `MBReplyCreator` followed this shape.

Three problems followed from this split:

1. **`skipped` was structurally untrackable in Pattern B.** If a Pattern B Creator gained a catch-and-continue loop, the ResourceCommand would see `list.size() < requested` and be unable to distinguish "skipped" from "errored". A lost diagnostic was one careless commit away.
2. **Pattern A Creators each reimplemented the `{success, count, requested, skipped, items, error?}` assembly.** Eight copies of the same JSON-building logic meant eight opportunities for a field rename or invariant violation (e.g. writing `error` only inside an `if` branch, so a future `continue` silently drops it).
3. **Workflow adapters duplicated the same logic a third time** to produce `WorkflowStepResult`. The adapter for each Creator type had to know whether the Creator returned `JSONObject`, `List<T>`, or `BatchResult<T>` and branch accordingly.

The three-pattern taxonomy was documented in `.claude/rules/writing-code.md` with the explicit warning "do not assume which pattern a Creator follows from its name or from documentation — verify by reading the return type". That warning was itself evidence that the taxonomy had failed: the patterns were not discoverable from the code.

## Decision

Every batch Creator's `create(...)` method returns `BatchResult<T>`. The contract — `{success, count, requested, skipped, items, error?}` — is enforced once, by the `BatchResult` record's canonical constructor. The ResourceCommand and workflow adapter each have a single helper that consumes the `BatchResult` and emits the appropriate downstream shape.

### Structure

`BatchResult<T>` record (in `com.liferay.support.tools.service`):

- Fields: `boolean success, int count, int requested, int skipped, List<T> items, String error`.
- Canonical constructor invariants:
	- `requested > 0`
	- `count >= 0` and `skipped >= 0`
	- `success == false` requires a non-blank `error` (a blank string is rejected)
	- `success == true` requires `count == requested`
	- `items` is defensively copied via `List.copyOf`
- Factory methods: `BatchResult.success(requested, items, skipped)` and `BatchResult.failure(requested, items, skipped, error)`. Both derive `count` from `items.size()`.

`ResourceCommandUtil.toJson(BatchResult<T>, Function<T, JSONObject>)` is the single entry point for building the wire response. It emits `error` only when `success == false`, avoiding a stray `"error": null` on success.

`WorkflowResultNormalizer.normalize(BatchResult<T>, Function<T, Map<String, Object>>)` is the single entry point for building a `WorkflowStepResult`. It is deliberately `public` because taxonomy, message-boards, and content workflow adapters live in sibling packages under `workflow.adapter.*` and all need to call it.

### One documented exception: `WebContentCreator`

`WebContentCreator` batches across multiple sites. Its `BatchResult<WebContentPerSiteResult>` carries one item per site (not per article), because flattening would lose per-site failure attribution — the UI could no longer display "Site A succeeded, Site B failed". Because `count` is total articles created across all sites, not `items.size()`, `WebContentCreator._buildBatchResult` uses the `BatchResult` canonical constructor directly rather than the `success()`/`failure()` factories.

`WebContentPerSiteResult` (extracted as a top-level record in `com.liferay.support.tools.service`) enforces its own mirror invariant: `failed > 0` requires a non-blank `error`, and `error` requires `failed > 0`. This guarantees the per-site rows remain consistent with the outer `BatchResult`'s success/error contract.

## Consequences

### Benefits

- **One place enforces the contract.** The `BatchResult` canonical constructor is the single point where success/error invariants are checked. A future `continue` path inside a Creator cannot silently drop the error field, because the constructor throws.
- **`skipped` is first-class everywhere.** Catch-and-continue loops in any Creator can now track skipped entries without ResourceCommand changes.
- **Adapter code shrinks.** ResourceCommands that previously hand-built JSON payloads (Pattern A) or recomputed counts (Pattern B) collapse to a single `ResourceCommandUtil.toJson(result, mapper)` call. Workflow adapters collapse similarly to a single `WorkflowResultNormalizer.normalize(result, mapper)` call.
- **Dead code deleted.** Intermediate step-item classes like `MBCategoryStepItem` and `MBThreadStepItem`, which only existed to adapt Pattern B lists into `WorkflowStepResult`, are gone. Workflow adapters now use inline lambdas to map `BatchResult` items.

### Trade-offs

- **Creators lose direct control of the JSON wire format.** A Creator that wanted a special field must now either add the field to its item type's `toJSONObject()` or extend `ResourceCommandUtil.toJson`. The `WebContentCreator`/`WebContentPerSiteResult` path is the escape hatch pattern when the outer contract genuinely needs an additional layer.
- **A "test-only" pure Creator invocation no longer returns a wire-ready shape.** This was already true for Pattern B Creators; tests that assert on the JSON shape go through the ResourceCommand or workflow adapter, not the Creator directly.
- **Migrating an existing Creator is non-trivial.** Pattern A Creators had to stop building JSON and start returning a typed list; Pattern B Creators had to start tracking `skipped` if they had a catch-and-continue loop.

## Alternatives considered

### Leave Pattern A as-is, migrate only Pattern B

Would have cut migration cost but preserved the multi-pattern ambiguity and the "verify the return type by reading the code" instruction. Rejected because the ambiguity itself was the problem — the three-pattern rule was not derivable from Creator names or method signatures without reading the body.

### Introduce a sealed interface `CreatorResult` with variants

Would allow exhaustive pattern matching in Java, but Java records with default values already model the sum type (success factory vs failure factory) cleanly enough, and the sealed-interface approach would double the number of types without adding enforcement the constructor doesn't already provide.

### Keep `JSONObject` as the return type, add `skipped` via helper

A thin helper like `JSONObject buildBatchResponse(...)` would unify Pattern A's manual assembly but would not touch Pattern B. The benefit of unification depends on Pattern B Creators also adopting it, which requires a typed intermediate anyway — which is exactly `BatchResult<T>`.

## References

- `BatchResult<T>` record design
- `ResourceCommandUtil.toJson` helper
- Pattern B migration (Vocabulary, Category, MBCategory, MBThread, Company)
- Pattern A migration (User, Organization, Role, Site, Layout, Document, WebContent)
- Layer dependency fix: `DOCUMENT_TEMP_FOLDER_NAME` moved to `LDFPortletKeys`
- `BlogsCreator` Pattern A → `BatchResult<T>` migration; 12 ResourceCommands unified to `PortletJsonCommandTemplate`; `CreatorJsonNormalizer` dead code removed
- `.claude/rules/writing-code.md` § "Creator return type is always `BatchResult<T>`"
