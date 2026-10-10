# Contributing

Thanks for helping. This page is the contributor workflow and the review checklist; everything else is linked from the [documentation map](docs/README.md).

## Workflow

1. Set up and build: [development guide](docs/guides/development.md).
2. Read the architecture page for the area you change: [overview](docs/architecture/overview.md), [backend](docs/architecture/backend.md), [frontend](docs/architecture/frontend.md). New entity type? Follow [adding an entity](docs/guides/adding-an-entity.md).
3. Change code, tests and documentation together.
4. Run the checks locally:

   ```bash
   ./gradlew :modules:liferay-dummy-factory:test
   (cd modules/liferay-dummy-factory && yarn test && yarn checkFormat)
   node scripts/check-docs.mjs
   ./gradlew :integration-test:integrationTest      # needs Docker + DXP license
   ```

5. Open a PR against `master`. CI runs unit tests and the docs check on every PR, and the integration suite on PRs to `master`.

Significant design decisions get an [ADR](docs/adr/README.md).

## Review checklist

Review contracts before "does it work", in this order. A contract violation is a regression even when tests are green.

1. **Input boundary policy** — new user input is validated and *rejected* at the resource command or Creator boundary, never silently rewritten; externally generated data is *sanitized*; the two strategies are not mixed. ([policy](docs/architecture/backend.md#input-boundary-policy))
2. **Single source of truth** — no fact, rule or contract duplicated across files; existing entries are edited, not copied. Versions are not copied into docs. ([rules](.claude/rules/documentation.md))
3. **Creator pattern** — `throws Throwable`; per-entity `BatchTransaction.run(...)`; validation outside the transaction; the resource command goes through `PortletJsonCommandTemplate`. ([creators](docs/architecture/backend.md#creators))
4. **Batch response contract** — `BatchResult<T>` with `{success, count, requested, skipped, items, error?}`; `success` is `count == requested`; `error` iff failure; frontend `parseResponse` checks `success === false || error`. ([contract](docs/architecture/backend.md#batch-response-contract))
5. **Parity** — resource command and workflow adapter expose identical item fields; new `/ldf/*` commands are registered in `view.jsp`. ([parity](docs/reference/workflow-api.md#rc--workflow-adapter-field-parity))
6. **Tests** — both branches of every new `if/else`; RNG output paired with deterministic locks; response shape asserted on success and failure paths. ([test rules](.claude/rules/tests.md))
7. **JSONWS-first verification** — database post-conditions through JSONWS; Playwright only when the DOM is the subject, with success-class assertions and no visible-text selectors. ([playwright](docs/reference/playwright.md))
8. **Documentation** — updated in the same PR (feature list, compatibility table, reference tables); `node scripts/check-docs.mjs` passes.

Also reject:

- abstractions "for future use" without a current consumer;
- try/catch around internal code "just in case" (validate at boundaries instead);
- comments that restate the code;
- `as any` / `as unknown as` without justification (the minimal-shape hook mock in tests is the documented exception);
- a hyphenated key-like string in a localized-text assertion;
- a second package-manager lockfile next to `yarn.lock`.

## Commits and pull requests

- One logical change per commit, with a message that says what and why.
- Releases (`latest/` JAR) follow the [release guide](docs/guides/release.md); ordinary PRs do not touch `latest/`.
