# ADR-0011: Use `AGENTS.md` as the single source of agent instructions

## Status

Accepted

## Date

2026-10-10

## Context

Agent instructions lived in Claude Code–specific places: a root `CLAUDE.md`, path-scoped rules in `.claude/rules/*.md` (loaded through Claude-only `paths:` front matter) and skills in `.claude/skills/`. Codex, which the project also uses, reads none of these: it reads `AGENTS.md` files and repository skills under `.agents/skills/`. A Codex session therefore worked without any of the contracts in this repository — most of which fail silently when broken (see the root `AGENTS.md`).

Constraints:

- `AGENTS.md` is the cross-tool convention (Codex, GitHub Copilot, Cursor, Gemini CLI and others); directory scoping by nested files is its only portable scoping mechanism.
- Claude Code reads `CLAUDE.md`; it reads `AGENTS.md` natively only in recent versions and only when no `CLAUDE.md` exists. Its documented way to share instructions is a `CLAUDE.md` that imports `@AGENTS.md`.
- Codex loads `AGENTS.md` files only from the repository root down to its working directory and truncates the chain at 32 KiB by default.
- Agent Skills is an open format; Codex discovers repository skills in `.agents/skills/`, Claude Code in `.claude/skills/`, where an entry may be a symlink.

## Decision

1. `AGENTS.md` files are the only hand-edited instruction files. The root file is the entry point; directory-scoped files sit next to the code they govern (module, Java, resources, both unit-test trees, `integration-test/`, `configs/`, `docs/`, `.github/`) and replace `.claude/rules/`.
2. The root `AGENTS.md` indexes every scoped file and instructs the agent to read the files on the path to anything it edits, so tools that do not auto-load deeper files still see them.
3. Every `AGENTS.md` has a sibling `CLAUDE.md` containing only `@AGENTS.md`.
4. Skills live in `.agents/skills/` with open-standard front matter; `.claude/skills/<name>` are per-skill symlinks to them.
5. `scripts/check-docs.mjs` enforces 2–4, the 32 KiB chain budget, and the absence of tool-specific rule files, so the layout cannot drift. Details: [agent instructions reference](../reference/agent-instructions.md).

## Alternatives considered

- **Keep `CLAUDE.md` and add a copy as `AGENTS.md`** — two sources that drift; the scoped rules would still be Claude-only.
- **`CLAUDE.md` → `AGENTS.md` symlinks** — break on Windows checkouts, and some editors and agents refuse to write through them. The one-line import costs nothing.
- **Rely on Claude Code's native `AGENTS.md` support** — version-dependent and disabled whenever any `CLAUDE.md` exists, including a personal one.
- **Keep `.claude/rules/` with `paths:` front matter** — more precise file-glob scoping, but invisible to every other tool. Directory scoping fits this repository's layout, and the few root-level files (build and dependency files) are covered in the root file.
- **A single large root `AGENTS.md`** — every session would pay for rules it does not need, and it would approach the Codex size limit.

## Consequences

### Positive

- Codex, Claude Code and other `AGENTS.md`-aware tools follow the same contracts from the same text.
- Rules load only where they apply, keeping each session's context small.
- The checker turns layout mistakes (an unindexed scoped file, a missing shim, an oversized chain) into CI failures instead of silently ignored instructions.

### Negative

- Ten one-line `CLAUDE.md` shims and per-skill symlinks are extra files; symlinks need `core.symlinks` on Windows.
- Scoping is by directory, not by glob, so rules for root-level files sit in the root `AGENTS.md`.

## References

- Root [`AGENTS.md`](../../AGENTS.md), [agent instructions reference](../reference/agent-instructions.md), `scripts/check-docs.mjs`.
- Claude Code memory and skills documentation: <https://code.claude.com/docs/en/memory>, <https://code.claude.com/docs/en/skills>.
- Codex `AGENTS.md` guide: <https://developers.openai.com/codex/guides/agents-md>; Agent Skills specification: <https://agentskills.io/specification>.
