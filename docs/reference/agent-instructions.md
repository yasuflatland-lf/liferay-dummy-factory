# Agent instructions reference

How the instructions for AI coding agents are laid out, which tool reads what, and what `node scripts/check-docs.mjs` enforces. The decision behind the layout is [ADR-0011](../adr/0011-agents-md-single-source.md). What goes *into* an instruction file is governed by [`docs/AGENTS.md`](../AGENTS.md).

## Layout

| Path | Role | Edited by hand |
|---|---|---|
| `AGENTS.md` (root) | entry point: project, commands, contracts, index of scoped files and skills, definition of done | yes |
| `<dir>/AGENTS.md` | rules for the subtree rooted at `<dir>`; listed in the root index | yes |
| `<dir>/CLAUDE.md` | Claude Code adapter; exactly the line `@AGENTS.md` | no |
| `.agents/skills/<name>/SKILL.md` | a procedure in the [Agent Skills](https://agentskills.io) format | yes |
| `.claude/skills/<name>` | Claude Code adapter; symlink to `../../.agents/skills/<name>` | no |
| `.agents/plan/` | scratch space, git-ignored | — |
| `AGENTS.override.md`, `CLAUDE.local.md` | personal instructions, git-ignored | — |

There are no tool-specific rule files (`.claude/rules/`, `.cursor/rules/`, `.github/copilot-instructions.md`, …). A rule that only one tool can see is a rule the other tools silently break.

## How each tool loads them

| | Codex | Claude Code |
|---|---|---|
| Root instructions | `AGENTS.md`, read at start | `CLAUDE.md` at start, which imports `AGENTS.md` |
| Scoped instructions | `AGENTS.md` files from the repository root down to the working directory, at start; deeper ones only if the agent opens them | `CLAUDE.md` in a subdirectory, on demand when a file in that subtree is read; it imports its sibling `AGENTS.md` |
| Precedence | deeper file wins | deeper file wins |
| Skills | `.agents/skills/` | `.claude/skills/` (the symlinks) |
| Size limit | the concatenated chain is truncated at `project_doc_max_bytes` (default 32 KiB) | none enforced |

Because Codex does not load an `AGENTS.md` below the directory it was started in, the root file lists every scoped file and tells the agent to read the ones on the path to any file it edits. Claude Code reads `AGENTS.md` natively only in recent versions and only when no `CLAUDE.md` is present; the one-line `@AGENTS.md` shim works in every version and with every other `CLAUDE.md`-only tool.

Start either tool at the repository root. Claude Code started in a subdirectory asks once before following an `@AGENTS.md` import that lies outside its working directory, and in non-interactive mode (`claude -p`) it skips those imports — so the ancestor rules are missing.

## Adding or changing instructions

- **A rule for one area:** add it to the `AGENTS.md` that owns the directory. For a new area, create `<dir>/AGENTS.md`, a `<dir>/CLAUDE.md` containing `@AGENTS.md`, and a row in the root index.
- **A new skill:** create `.agents/skills/<name>/SKILL.md` with `name` (equal to `<name>`; lowercase letters, digits and single hyphens; at most 64 characters) and `description` (what it does and when to use it; at most 1024 characters) in the front matter, then `ln -s ../../.agents/skills/<name> .claude/skills/<name>` and a row in the root skills table. Keep the front matter to the open-standard keys so every tool accepts it.
- **On Windows**, clone with `git config core.symlinks true` (and Developer Mode enabled) so the `.claude/skills/` links are real symlinks.

## What the checker enforces

`node scripts/check-docs.mjs`, run in CI, fails when:

- an `AGENTS.md` has no sibling `CLAUDE.md`, a `CLAUDE.md` has no sibling `AGENTS.md`, or a `CLAUDE.md` contains anything but `@AGENTS.md`;
- a scoped `AGENTS.md` is not linked from the root `AGENTS.md`;
- the chain of `AGENTS.md` files from the root to any directory exceeds 32 KiB (Codex would silently drop the rest);
- a skill's front matter is invalid or its `name` differs from its directory, a skill has no `.claude/skills/` symlink, a symlink points anywhere else, or a skill is missing from the root skills table;
- a tool-specific rule directory or file (`.claude/rules/`, `.cursor/rules/`, `.github/copilot-instructions.md`) exists.

Its other checks (links, anchors, quoted paths, ADR index, versions in prose) apply to these files like to any other Markdown file.
