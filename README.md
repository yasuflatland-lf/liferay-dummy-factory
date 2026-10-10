# Liferay Dummy Factory

[![Unit Tests](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/unit-test.yml/badge.svg?branch=master)](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/unit-test.yml)
[![Integration Tests](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/integration-test.yml/badge.svg?branch=master)](https://github.com/yasuflatland-lf/liferay-dummy-factory/actions/workflows/integration-test.yml)
[![codecov](https://codecov.io/gh/yasuflatland-lf/liferay-dummy-factory/branch/master/graph/badge.svg)](https://codecov.io/gh/yasuflatland-lf/liferay-dummy-factory)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Generate realistic test data in Liferay in seconds — users, sites, pages, web content, documents and more — from a Control Panel app or a JSON workflow.

> [!WARNING]
> Dummy Factory is a development and testing tool. Do not install it on a production system.

## Features

- **Batch creation for 14 entity types**: organizations, roles, users, sites, pages, web content, documents, blogs, vocabularies, categories, message-board categories, threads and replies, and companies (virtual instances).
- **Realistic content**: optional Datafaker-generated names and text in several locales, site templates, tags, uploaded template files.
- **Workflows**: one JSON document creates a whole scenario — e.g. a site, its pages and its web content — with ids flowing from step to step. Plan (validate) before you execute. [Learn more](docs/guides/workflows.md).
- **Claude Code (MCP)**: Liferay's built-in MCP Server exposes the workflow API as tools, so Claude Code can create dummy data from a prompt. [Set it up](docs/guides/mcp-setup.md).
- **Honest results**: a batch reports exactly what was requested, created and skipped, and fails loudly instead of silently creating less.

## Compatibility

Pick the JAR that matches your Liferay version and drop it into `${LIFERAY_HOME}/deploy/`.

| Liferay | Download |
|---|---|
| DXP 2026.Q3.6 | [`master/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/master/latest) |
| DXP 2026.Q1.9 LTS | [`2026.Q1.9-LTS/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2026.Q1.9-LTS/latest) |
| DXP 2026.Q1.3 LTS | [`2026.Q1.3-LTS.1/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2026.Q1.3-LTS.1/latest) |
| DXP 2025.Q1.14 LTS | [`2025.Q1.14-LTS/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2025.Q1.14-LTS/latest) |
| DXP 2025.Q1.11 | [`2025.Q1.11/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2025.Q1.11/latest) |
| DXP 2024.Q3.9 | [`2024.q3.9/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2024.q3.9/latest) |
| DXP 2024.Q1.12 | [`2024.q1.12/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2024.q1.12/latest) |
| DXP 2023.Q4.9 | [`2023.q4.9/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/2023.q4.9/latest) |
| 7.4 | [`7.4.4/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.4.4/latest) |
| 7.3 | [`7.3.x/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.3.x/latest) |
| 7.2 | [`7.2.x/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.2.x/latest) |
| 7.1 | [`7.1.x/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.1.x/latest) |
| 7.0 | [`7.0.x/latest`](https://github.com/yasuflatland-lf/liferay-dummy-factory/tree/7.0.x/latest) |

Older branches may offer fewer features than `master`.

## Quick start

1. Copy the JAR into `${LIFERAY_HOME}/deploy/` and wait for `STARTED liferay.dummy.factory_` in the log.
2. Sign in as an administrator and open **Control Panel → Liferay Dummy Factory**.
3. Choose an entity type, set how many to create, and submit — or switch to the **Workflow JSON** tab and load a sample.

Full instructions: [Install and use](docs/guides/installation.md).

## Development

```bash
export LIFERAY_DXP_LICENSE_FILE=/path/to/activation-key.xml   # DXP activation key
./gradlew :modules:liferay-dummy-factory:jar                  # build the bundle
./gradlew startDockerContainer                                # run DXP with the bundle on http://localhost:8080
./gradlew :integration-test:integrationTest                   # end-to-end tests
```

Requires JDK 21, Node and Yarn (see `mise.toml`), and Docker. Start with the [development guide](docs/guides/development.md) and [CONTRIBUTING.md](CONTRIBUTING.md). All documentation: [docs/](docs/README.md).

## License

[MIT](LICENSE)
