---
name: contract-review
description: Review a diff or pull request in this repository against its contracts (input boundary, single source of truth, Creator/BatchResult contract, test coverage, JSONWS-first verification, documentation). Use when asked to review code or a PR, or before declaring a change complete.
---

# Contract review

Review against the checklist in `CONTRIBUTING.md` (section "Review checklist"), in its order: contract violations come before "does it work". For each finding, cite the file and line and the contract it breaks, linking the doc that defines it.

Also run, and report the result of:

```bash
node scripts/check-docs.mjs
./gradlew :modules:liferay-dummy-factory:test
(cd modules/liferay-dummy-factory && yarn test)
./gradlew :integration-test:compileTestGroovy   # when specs changed
```

The integration suite needs Docker and a DXP license; if you cannot run it, say so instead of implying it passed.
