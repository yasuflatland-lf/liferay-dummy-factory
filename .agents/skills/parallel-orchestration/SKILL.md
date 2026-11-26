---
name: parallel-orchestration
description: Coordinate multi-agent work in this repository — one coordinating agent, parallel worker sub-agents partitioned by file cluster, and a single serial commit agent. Use only when the user explicitly asks for parallel or multi-agent orchestration.
---

# Parallel orchestration

Works with any agent tool that can spawn sub-agents (Claude Code sub-agents, Codex sub-agents, …). Every agent obeys the `AGENTS.md` files on the path to each file it touches.

## Roles

- **Coordinator** — decomposes the task, dispatches sub-agents, aggregates their reports, decides the next phase. It does not read, edit or run code itself; its context holds plans and summaries, not source.
- **Workers** — one scoped unit of work each, in parallel when their file clusters do not overlap. Workers do not spawn agents, stage, commit or push.
- **Commit agent** — the only writer of git history; runs serially.

## Phases

1. **Explore** — read-only workers map what exists, where, and how it is wired. Output: a summary with repository paths.
2. **Design** — the coordinator turns that into a task list partitioned by file cluster, with owners and dependencies. Two workers never own the same path; overlapping work is serialized.
3. **Implement** — workers edit their clusters in parallel and report a diff summary. The coordinator waits for the whole wave before starting the next.
4. **Review → simplify → verify** — one worker reviews the diffs (use the `contract-review` skill), one removes dead code and redundancy, one runs the real build and test commands from the root `AGENTS.md`. Only a green verify lets the commit agent run.

## Commits

- One logical change per commit; the commit agent splits unrelated fixes.
- Commit messages are one line. No `Co-Authored-By` trailers, no body text, no emoji.

## Context hygiene

When a worker's context is nearly full, have it compact or summarize before continuing (whatever the tool provides). Keep discovered solutions, final decisions and the paths that matter; drop debug transcripts, failed attempts and raw tool output.

## Merging a base branch

1. `git fetch origin && git merge origin/<base-branch>`; list the conflicting files.
2. **Ask the user which side is authoritative** before any `git checkout --ours` / `--theirs` — both silently drop the other side.
3. Resolve `package.json` and `yarn.lock` from the same side, never mixed, then `yarn install` to re-sync the lockfile.
4. A clean auto-merge of a lockfile can still break the dependency graph: rerun the unit suites (and the integration suite when available) before the commit agent commits and pushes.

## Anti-patterns

- The coordinator opening files "just to double-check" — dispatch a worker.
- More than one agent committing.
- Skipping verify because "the diff looks right".
- Workers spawning workers.
