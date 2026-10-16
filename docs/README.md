# Documentation

Organized by what you are trying to do ([Diátaxis](https://diataxis.fr/)): **guides** for tasks, **reference** for facts and contracts, **architecture** for understanding, **ADRs** for past decisions. Each fact lives in exactly one page; other pages link to it.

## Using Dummy Factory

| Page | Read it to… |
|---|---|
| [Install and use](guides/installation.md) | deploy the JAR and create data from the Control Panel |
| [Workflows](guides/workflows.md) | create related entities in one run from JSON |
| [Claude Code (MCP)](guides/mcp-setup.md) | create dummy data from Claude Code prompts through Liferay's MCP Server |

## Developing

| Page | Read it to… |
|---|---|
| [Contributing](../CONTRIBUTING.md) | follow the workflow and the review checklist |
| [Development setup](guides/development.md) | build the bundle and run DXP locally |
| [Running the tests](guides/testing.md) | run unit, frontend and integration tests |
| [Adding an entity type](guides/adding-an-entity.md) | add a new kind of dummy data end to end |
| [Releasing](guides/release.md) | publish the `latest/` JAR and manage version branches |
| [Troubleshooting](guides/troubleshooting.md) | go from a symptom to its cause and fix |

## Understanding the system

| Page | Covers |
|---|---|
| [Architecture overview](architecture/overview.md) | repository layout, runtime components, request flows, test layers |
| [Backend](architecture/backend.md) | Creators, `BatchResult`, resource commands, input boundary policy, workflow adapters |
| [Frontend](architecture/frontend.md) | esbuild bundle, React app, i18n, Workflow JSON editor |

## Reference

| Page | Covers |
|---|---|
| [Workflow API](reference/workflow-api.md) | `/o/ldf-workflow` contract, operations, field parity, samples |
| [Liferay DXP API](reference/liferay-dxp-api.md) | changed signatures, JSONWS exposure, headless quirks |
| [DXP runtime configuration](reference/dxp-runtime-config.md) | `configs/common/`, Basic Auth, SAP, `.config` syntax |
| [Test harness](reference/test-harness.md) | Gradle task graph, container, deployment, `BaseLiferaySpec`, coverage, CI |
| [Playwright](reference/playwright.md) | selectors, waiting, assertion patterns |
| [Vitest](reference/vitest.md) | setup, i18n guard, mocking, parity tests |
| [`data-testid` contract](reference/test-ids.md) | generated and fixed test ids |
| [Dependencies](reference/dependencies.md) | where versions are pinned, constraints between pins, upgrades |
| [Agent instructions](reference/agent-instructions.md) | `AGENTS.md` layout, Codex and Claude Code loading, skills, what the checker enforces |

## Decisions

[Architecture Decision Records](adr/README.md).

## For AI agents

The root [`AGENTS.md`](../AGENTS.md) is the entry point for every agent (Codex, Claude Code, …). Directory-scoped `AGENTS.md` files, indexed there, hold the rules for their subtree; task procedures are skills in `.agents/skills/`. Each `CLAUDE.md` is a one-line import of its sibling `AGENTS.md`. Rules stay short and link here for explanations. Layout and loading: [agent instructions](reference/agent-instructions.md).

Keeping docs correct: [`docs/AGENTS.md`](AGENTS.md) defines where things go; `node scripts/check-docs.mjs` (run in CI) verifies links, anchors, quoted paths, the ADR index, that no pinned version is copied into prose, and the agent-instruction layout.
