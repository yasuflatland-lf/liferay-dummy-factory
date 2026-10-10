# Architecture Decision Records

Each ADR records one significant decision: the context, the choice, and its consequences. ADRs are immutable once accepted — when a decision changes, write a new ADR and mark the old one *Superseded*; only the Status line and links of an old ADR are edited.

New ADR: copy [`template.md`](template.md) to `NNNN-short-title.md` with the next free number, and add a row below.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-integration-test-architecture.md) | Integration test architecture | Accepted; container strategy superseded by 0008 |
| [0002](0002-portlet-api-javax-namespace.md) | Use `javax.portlet` 3.0 (CE 7.4) | Superseded by 0008 |
| [0003](0003-playwright-selector-strategy.md) | Playwright selectors: `data-testid` with `getByRole` | Accepted |
| [0004](0004-github-actions-version-policy.md) | GitHub Actions version follow-up policy | Superseded in part: Renovate now proposes action bumps |
| [0005](0005-node-vitest-version-pinning.md) | Exact pins for the Vitest toolchain | Accepted; the Node 20.12 context no longer applies, the exact-pin rule stands |
| [0006](0006-custom-esbuild-over-npm-scripts.md) | Custom esbuild pipeline instead of `@liferay/npm-scripts` | Accepted |
| 0007 | — | Number not used |
| [0008](0008-dxp-2026-migration.md) | Migrate to Liferay DXP 2026 | Accepted |
| [0009](0009-unified-batch-result.md) | Unify all batch Creators on `BatchResult<T>` | Accepted |
| [0010](0010-mcp-via-liferay-mcp-server.md) | Expose data creation through Liferay's MCP Server | Proposed |

Version numbers, file paths and plugin versions inside an ADR describe the moment of the decision. For the current state, follow the links to `docs/reference/` and `docs/architecture/`.
