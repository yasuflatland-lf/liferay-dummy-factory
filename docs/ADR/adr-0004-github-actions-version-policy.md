# ADR-0004: GitHub Actions Plugin Version Follow-up Policy

## Status

Accepted

## Date

2026-04-11

## Context

The CI workflows under `.github/workflows/` pin third-party action versions by major tag (e.g. `actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`). GitHub rolls these majors on a yearly-ish cadence, and each major bump typically deprecates the previous one: `upload-artifact@v3` was fully retired in early 2025, `v4` followed a similar path, and by the time this ADR was written the current majors had already moved past what was originally pinned. Pinned majors do not auto-upgrade, so a workflow that was green a year ago can break overnight when GitHub finally disables the deprecated tag.

We do not want Dependabot to flood the repo with minor/patch PRs for every action, but we also cannot let the pins drift silently until CI explodes.

## Decision

Whenever a pull request touches `.github/workflows/**`, the author (or the reviewing agent) must check that every third-party action pin is still on a supported major. The check is mechanical:

```bash
gh api repos/<org>/<repo>/releases/latest --jq '.tag_name'
# e.g.
gh api repos/actions/upload-artifact/releases/latest --jq '.tag_name'
gh api repos/actions/setup-java/releases/latest   --jq '.tag_name'
gh api repos/actions/checkout/releases/latest     --jq '.tag_name'
```

If the latest release is a newer major than what is pinned in the workflow, bump the pin in the same PR. If the pinned major is still the latest, leave it. Minor/patch drift is ignored — we pin on major only.

This is a habit, not an automation. We do not add Dependabot, Renovate, or a scheduled workflow for it. The check runs at the one moment where the cost of upgrading is already amortized: a human is already editing the workflow file.

## Alternatives Considered

- **Dependabot on `.github/workflows`** — rejected. Produces a steady stream of PRs for minor/patch bumps that we do not care about, and the signal-to-noise ratio trains reviewers to ignore action bumps entirely — exactly the opposite of what we want.
- **Pin to full SHA** — rejected. Supply-chain-safer, but makes routine version reads impossible without dereferencing the SHA, and the repo is not a high-value target that justifies the overhead.
- **Pin to `@main` / `@latest`** — rejected. Non-reproducible builds; a third party can break us at any time.

## Consequences

### Positive

- No surprise CI outages from a deprecated major being disabled server-side.
- Workflow edits stay low-churn; no bot PRs to triage.
- The check is cheap (three `gh api` calls) and only runs when a workflow is already being touched.

### Negative

- Relies on discipline. A PR that edits a workflow without bumping a stale action slips through unless a reviewer notices.
- Does not catch the case where no one touches `.github/workflows/**` for a full year while a major is retired underneath us. Mitigation: if CI fails with a "this version of the runner is deprecated" message, that is the signal to sweep all action pins at once.

## References

- `.github/workflows/integration-test.yml`
- `.github/workflows/unit-test.yml`
