# liferay-dummy-factory

[![Unit Tests](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/unit-test.yml/badge.svg?branch=master)](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/unit-test.yml)
[![codecov](https://codecov.io/gh/yasuflatland-lf/liferay-dummy-factory/branch/master/graph/badge.svg)](https://codecov.io/gh/yasuflatland-lf/liferay-dummy-factory)

Dummy Factory generates dummy data for debugging use. Please don't use this for a production use.

## What does Dummy Factory generate?

* Organizations
* Sites
* Pages
* Users
* Web Content Articles
* Documents
* Message Board (Threads / Categories)
* Category (Categories / Vocabularies)
* Wiki
* Blogs
* Company
* Workflows — chain the generators above into a single run

### Workflows

A **Workflow** is a JSON document that orchestrates several of the generators above into one declarative run. Each step calls an operation (`company.create`, `site.create`, `layout.create`, `user.create`, …) with its own parameters, and later steps can pull values from earlier ones via `from` references such as `"steps.createSite.items[0].groupId"` — so the generated `groupId` of a Site is fed straight into the Page creation step with no manual copy-paste.

Why it's useful:

- **One submit, full scenario.** Stand up realistic fixtures (Company → Users → Organization, or Site → Pages → Web Content) without clicking through the Control Panel form-by-form.
- **Cross-step wiring.** Dependent IDs (`groupId`, `companyId`, primary keys) flow automatically between steps, so you never have to look up an ID and paste it into the next form.
- **Per-step error policy and idempotency.** Each step carries an `idempotencyKey` and an `onError` policy (e.g. `FAIL_FAST`), making reruns safe and failures diagnosable.
- **Reproducible environments.** Workflow JSON is plain text — commit it, share it, replay it. The same sample files drive both the React UI and the Spock integration tests, so what you run locally is exactly what CI verifies.

Ready-made samples live under `integration-test/src/test/resources/workflow-samples/` (e.g. `company-user-organization.json`, `site-and-page.json`, `blogs-and-web-content.json`).

## Required environment
* Java 21 or above
* Liferay DXP 2026.Q1.3-LTS (this branch)
* Liferay 7.4 (please see the 7.4.x branch)
* Liferay 7.3 GA1 (please see the 7.3.x branch)
* Liferay 7.2 (please see the 7.2.x branch)
* Liferay 7.1 (please see the 7.1.x branch)
* Liferay 7.0 (please see the 7.0.x branch)

> For development rules and contracts, start with [`CLAUDE.md`](CLAUDE.md) and the task-based files under [`.claude/rules/`](.claude/rules/). Concrete details live under [`docs/details/`](docs/details/), and architectural decisions in [`docs/ADR/`](docs/ADR/).


## Tech Stack

| Layer | Technology |
|-------|------------|
| Portal | Liferay DXP 2026.Q1.3-LTS |
| Backend | MVCPortlet + MVCResourceCommand (layered) |
| Frontend | React + Clay CSS |
| Build | Gradle 8.5 + Liferay Workspace Plugin 10.1.9 |
| Testing | Spock 2.4 / Groovy 5.0 / Playwright 1.59.0 |
| Java | JDK 21 |

## Usage
| Version | Link                                                                                                                                                       | 
|---------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------| 
| 7.4     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) |
| 7.3     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest)   | 
| 7.2     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest)   | 
| 7.1     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest)   | 
| 7.0     | [https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest)   | 

1. Download jar file according to the version above and place it int `${liferay-home}/deploy ` 
1. Start Liferay bundle and login as an administrator.
1. After the jar is properly installed, navigate to `Control Panel -> System Settings -> Platform -> Thrid party` and enable JQuery.
1. Reboot the bundle.
1. Navigate to `Control Panel`, under `Marketplace`, `Dummy Factory` will be found.
1. Now you are ready to create dummy data! Enjoy!

## Quick Start (Docker via Workspace Plugin)

Provision the DXP 2026 activation key (required):

```bash
# Local: point to your file
export LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml

# Or CI: base64-encoded XML in an env var
export LIFERAY_DXP_LICENSE_BASE64="$(base64 -w0 /path/to/activation-key.xml)"
```

Start the container:

```bash
./gradlew startDockerContainer       # builds the image and boots Liferay on :8080
./gradlew stopDockerContainer        # stop (preserves state for next run by default)
./gradlew removeDockerContainer      # hard reset (forces image rebuild next start)
```

The workspace plugin manages everything — the old `docker run liferay/portal:...` workflow is no longer used.

### Verify Operation

Navigate to http://localhost:8080 and log in as admin (`test@liferay.com` / `test`).
**Liferay Dummy Factory** will appear under Control Panel > Configuration.

Check bundle status via GoGo Shell:

```bash
docker exec liferay bash -c "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"
```

## Build

```bash
# Module build
./gradlew :modules:liferay-dummy-factory:build
```

## Testing

Requires Docker to be running and a valid DXP activation key.

```bash
# Run all integration tests (inline license path)
LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml ./gradlew :integration-test:integrationTest --info

# CI: base64-encoded XML
export LIFERAY_DXP_LICENSE_BASE64="$(base64 -w0 /path/to/activation-key.xml)"

# Run all integration tests (if env var already exported)
./gradlew :integration-test:integrationTest --info

# Run a specific spec
LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml ./gradlew :integration-test:integrationTest --tests "com.liferay.support.tools.it.spec.DeploymentSpec"
```

The `integrationTest` task manages the DXP container lifecycle via the Liferay Workspace Plugin (no Testcontainers). It starts the container, deploys the bundle, waits up to 8 minutes for Liferay to become ready, runs the specs, then stops the container.

Specs:

- **DeploymentSpec** -- Bundle deployment and activation (via GoGo Shell)
- **PortletRenderSpec** -- Login and portlet rendering through the browser (Playwright)
- **OrganizationFunctionalSpec** -- Organization batch creation via portlet UI with REST API verification (Playwright)

## CI

GitHub Actions (`.github/workflows/integration-test.yml`) runs automatically on push / PR to `master`.
