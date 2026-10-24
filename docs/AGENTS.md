# Documentation

Scope: `docs/`. The rules in this file apply to **every** Markdown file in the repository (root files, `AGENTS.md` files, skills), not only to `docs/`. Also obey the root [`AGENTS.md`](../AGENTS.md).

The documentation map is [`README.md`](README.md). Before finishing any change to code or docs, run `node scripts/check-docs.mjs` (CI runs it too).

- **One fact, one file.** Before writing a rule, contract or gotcha, search for it (`grep -rn '<term>' --include='*.md' .`). If it exists, edit it there and link to it. Duplicates are review blockers.
- **Put it where its reader looks** (Diátaxis):
  - `docs/guides/` — task-oriented how-tos (install, develop, test, release, troubleshoot).
  - `docs/reference/` — facts and contracts to look up (APIs, configuration, test ids, dependencies).
  - `docs/architecture/` — explanations of how and why the system is built this way.
  - `docs/adr/` — decision records; immutable once accepted (only Status and links change). New ADR: copy `docs/adr/template.md`, next free number, add it to `docs/adr/README.md`.
  - `AGENTS.md` files — short, imperative rules for agents, scoped to their directory; they link to docs for explanations instead of repeating them. How they are organized: [`reference/agent-instructions.md`](reference/agent-instructions.md).
  - `.agents/skills/` — step-by-step procedures an agent runs on request.
- **No versions in prose.** Name the file that pins a version instead of the version (the checker rejects copies of pinned tool versions).
- **Links and paths must resolve.** Use relative Markdown links; quote real repository paths in backticks. The checker verifies both, plus anchors.
- **Update docs in the same change as the code** they describe — including the README feature list, the compatibility table and the reference tables (operations, parity, test ids).
- **Write plainly**: English, short sentences, tables for lookups, code blocks for commands. Assert facts you have verified in the code; do not describe aspirations as behaviour.
