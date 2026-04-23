# Parallel Orchestration Workflow

Template for running multi-agent work where the main Claude Code session coordinates and sub-agents execute.

## Roles

- **Main agent** — orchestration only. MUST NOT read, edit, or run code directly. Its job is to decompose the task, dispatch sub-agents, aggregate their reports, and decide the next phase.
- **Sub-agents** — execute one scoped unit of work each. Run in parallel whenever their file clusters are independent.
- **Commit agent** — a dedicated, single, serial agent responsible for every git commit. No other agent commits.

## Parallelism rules

- Dispatch sub-agents in parallel whenever their working sets do not overlap. Partition by file cluster, not by task phase.
- If two sub-agents would touch the same file, serialize them — never let two agents race the same path.
- The main agent waits for all parallel sub-agents in a wave to report before starting the next wave.

## Commit rules

- Commits are serialized through the single commit agent. Sub-agents stage nothing and push nothing.
- Commit messages are one line. No `Co-Authored-By` trailers, no body text, no emoji.
- One logical change per commit. If a sub-agent produced two unrelated fixes, the commit agent splits them.

## Context hygiene

- When a sub-agent's remaining context budget drops to 5% or below, run `/compact` before continuing. Preserve: discovered solutions, final decisions, file paths that matter. Drop: debug transcripts, failed attempts, raw tool output.
- The main agent's own context should stay lean — it holds plans and summaries, not source code.

## Four-phase pattern

1. **Explore** — one or more read-only sub-agents map the relevant code, answer "what exists, where, and how is it wired?". Output: a written summary with absolute file paths.
2. **Design** — main agent (or a dedicated design sub-agent) turns the exploration into a plan: file clusters, sub-agent assignments, dependencies. Output: a task list with clear ownership boundaries.
3. **Implement** — parallel sub-agents edit their clusters. Each reports completion with a diff summary.
4. **Review → Simplify → Verify** — a review sub-agent reads the diffs, a simplify sub-agent removes dead code / redundancy, and a verify sub-agent runs the actual build/test commands. Only after verify is green does the commit agent run.

## Anti-patterns

- Main agent opens files "just to double-check" — forbidden; dispatch a sub-agent.
- Multiple agents committing in the same session — forbidden; the commit agent is the only writer of git history.
- Skipping the verify step because "the diff looks right" — forbidden; green build is the gate.
- Sub-agents spawning their own sub-agents — forbidden; only the main agent dispatches.

## Merge Conflict Resolution

When merging a base branch (e.g. `origin/master`) into a feature branch produces conflicts, follow a deterministic pattern instead of hand-editing conflict markers.

### Pattern I23 — feature branch wins

Use when the feature branch is authoritative and base-branch changes should be discarded in the conflicting files:

1. `git fetch origin`
2. `git merge origin/<base-branch>`
3. Identify conflicting files from `git status`.
4. **Confirm with the user which side is authoritative** before running any `--ours` / `--theirs` — these flags are destructive and silently drop the other side's changes.
5. `git checkout --ours <file>` (or `--theirs <file>`) for each conflicting path.
6. `git add <file>`
7. `git commit --no-edit`
8. Run verification commands (see I24).
9. `git push <branch>`

### Pattern I24 — always re-run tests after a merge

Lock files (`yarn.lock`, `package-lock.json`, `pnpm-lock.yaml`) can auto-merge cleanly yet produce a broken dependency graph. Never trust a green merge — re-run the full unit and integration test suites before pushing.

### Lock-file + manifest triad

`package.json` and its lock file almost always conflict together. The canonical recovery is:

1. Resolve all three files (`package.json`, `yarn.lock` / `package-lock.json`) with the same side (`--ours` or `--theirs`) — never mix.
2. Run `yarn install` / `npm install` / `pnpm install` to rebuild lock-file consistency against the resolved `package.json`.
3. Re-run `yarn test` (unit + integration) to catch regressions from transitive dependency drift.
4. Only then stage, commit, and push.

### Step template

```
1. git fetch origin
2. git merge origin/<base-branch>
3. List conflicting files
4. Confirm with the user which branch is authoritative
5. git checkout --ours <file>   (or --theirs)
6. git add <file>
7. yarn install / pnpm install to re-sync lock files
8. yarn test / unit test / integration test to catch regressions
9. git commit --no-edit
10. git push <branch>
```

All merge-conflict commits still flow through the single commit agent — sub-agents never resolve conflicts and commit on their own.
