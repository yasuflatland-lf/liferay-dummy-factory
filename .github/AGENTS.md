# GitHub Actions

Scope: `.github/`. Also obey the root [`AGENTS.md`](../AGENTS.md). CI facts: [`docs/reference/test-harness.md`](../docs/reference/test-harness.md#ci); action pin policy: [ADR-0004](../docs/adr/0004-github-actions-version-policy.md).

- When editing a workflow, check each action pin is on its latest major (Renovate also proposes bumps).
- Tool versions in workflows follow the pin files listed in [`docs/reference/dependencies.md`](../docs/reference/dependencies.md#where-versions-live); do not introduce a second source.
- CI must keep running `node scripts/check-docs.mjs` — it is the guard that keeps the agent instructions and docs consistent.
