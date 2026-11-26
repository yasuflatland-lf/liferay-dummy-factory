# Portal and OSGi configuration

Scope: `configs/`. Also obey the root [`AGENTS.md`](../AGENTS.md). What each file does and why: [`docs/reference/dxp-runtime-config.md`](../docs/reference/dxp-runtime-config.md).

- `.config` files use typed literals (`B"true"`), at most one comment line, and comma-separated strings for `urlsIncludes`.
- `configs/common/portal-liferay-online-config.properties` stays empty.
- No empty per-environment `portal-ext.properties`.
- Never commit a DXP license (`configs/*/deploy/activation-key.xml`, `*.lic` are git-ignored).
