# ADR-0011: Replace `@liferay/npm-scripts` format with Prettier

## Status

Accepted; supersedes decision 2 of [ADR-0006](0006-custom-esbuild-over-npm-scripts.md)

## Date

2026-11-26

## Context

Dependabot reports many npm alerts in the transitive dependency tree of `@liferay/npm-scripts`. The module uses it only for formatting; production bundling already uses a custom esbuild script, as recorded in [ADR-0006](0006-custom-esbuild-over-npm-scripts.md).

## Decision

Remove `@liferay/npm-scripts` and use exact-pinned Prettier and `@liferay/prettier-plugin` dependencies. Keep the `format` and `checkFormat` script names. Match the portal's formatter through the configuration described in the dependency reference below. See the [dependency reference](../reference/dependencies.md#formatter) for configuration and the [development guide](../guides/development.md#format) for scope and commands.

Delete the unused Babel configuration. Keep `node-scripts.config.js` because the custom esbuild script reads its entry point.

## Alternatives considered

- **Keep the wrapper and override vulnerable transitives** — retains an unused build toolchain and requires ongoing dependency overrides just for formatting.
- **Adopt Prettier defaults** — causes unnecessary formatting changes to existing Liferay-style sources.

## Consequences

### Positive

- Removes the unused build toolchain and its transitive dependency tree.
- Keeps familiar formatter commands and explicit local configuration.

### Negative

- The formatter pins must stay aligned with the plugin's Prettier dependency; upgrades can change source formatting.

## References

- [Module dependencies and scripts](../../modules/liferay-dummy-factory/package.json)
- [Frontend architecture](../architecture/frontend.md)
