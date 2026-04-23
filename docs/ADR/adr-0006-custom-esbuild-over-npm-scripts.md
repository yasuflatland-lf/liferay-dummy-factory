# ADR-0006: Use Custom esbuild Pipeline Instead of @liferay/npm-scripts

## Status

Accepted

## Date

2026-04-12

## Context

The portlet's React frontend needs a JavaScript build pipeline that produces an ESM bundle compatible with Liferay CE 7.4 GA132's AMD module loader. Two options were evaluated:

1. **`@liferay/npm-scripts` (v51.x)** — Liferay's official Webpack-based build toolchain for frontend modules
2. **Custom esbuild script** (`scripts/build.mjs`) — a ~165-line build script using esbuild directly

### Why not `@liferay/npm-scripts`

| Concern | Detail |
|---------|--------|
| DXP-first assumptions | npm-scripts targets the portal monorepo and DXP release cadences. External workspace modules on CE 7.4 hit edge cases (dependency resolution, API version skew) |
| Build speed | npm-scripts wraps Webpack, which is significantly slower than esbuild for single-module builds |
| Unnecessary complexity | npm-scripts includes SASS processing, export bridge generation, TypeScript type-checking, and a full linker plugin for 200+ portal-global packages. This portlet needs none of those — it uses Clay CSS classes, has a single entry point, and only externalizes `react` and `react-dom` |
| Liferay's own direction | The portal monorepo has replaced npm-scripts with `@liferay/node-scripts` (esbuild-based). npm-scripts is the legacy path |
| Transparency | A 165-line build script is fully readable and debuggable. npm-scripts is a black-box pipeline with plugin chains that are difficult to trace when something breaks |

### What the custom build produces

The script outputs the same artifact structure that Liferay's module loader expects:

| Artifact | Path | Purpose |
|----------|------|---------|
| ESM bundle | `__liferay__/index.js` | esbuild output with react/react-dom externalized |
| AMD bridge | `index.js` | `Liferay.Loader.define()` wrapper that imports the ESM bundle |
| `manifest.json` | root | Declares `{esModule: true, useESM: true}` for the AMD loader |
| `package.json` | root | Module identity for Liferay's frontend registry |

## Decision

### 1. Use esbuild via `scripts/build.mjs` for production bundling

The build script handles four steps sequentially:
1. Bundle TSX/TS source into a single ESM file via esbuild
2. Generate an AMD bridge (`index.js`) that registers the module with `Liferay.Loader.define()`
3. Write `package.json` with module name and version
4. Write `manifest.json` with ESM flags

### 2. Retain `@liferay/npm-scripts` for formatting only

`package.json` scripts:
```json
{
  "build": "node scripts/build.mjs",
  "checkFormat": "liferay-npm-scripts format --check",
  "format": "liferay-npm-scripts format"
}
```

`@liferay/npm-scripts` remains a `devDependency` exclusively for its `format` command, which enforces Liferay's code style. It does not participate in the production build.

### 3. Externalize react and react-dom to Liferay's shared copies

A custom esbuild plugin (`liferayReactExternalsPlugin`) redirects `react` and `react-dom` imports to the portal's shared bundles at `../../frontend-js-react-web/__liferay__/exports/react.js`. This prevents React from being duplicated in the browser.

### 4. Two-mechanism i18n architecture: serve-time replacement + runtime JSP injection

Liferay's `LanguageUtil.process()` IS applied to the ESM bundle at serve-time. It scans the JavaScript source for `Liferay.Language.get('string-literal')` calls and replaces the string-literal argument with the resolved value from the portal's language bundles. However, for module-specific keys not present in the global language bundle, the replacement resolves to the key itself (the standard Liferay fallback behavior), which is indistinguishable from a missing key.

To make module-specific keys available for serve-time replacement, a `ResourceBundleLoader` must be registered so that the `LanguageResourcesExtender` merges them into the global resolution path (see Decision 5).

Variable-parameter calls such as `Liferay.Language.get(variable)` are not matched by the `LanguageUtil.process()` regex and survive to runtime unchanged. For these calls, `view.jsp` injects language keys from `portletConfig.getResourceBundle(locale)` into `Liferay.Language._cache` before the React component renders, providing runtime resolution.

### 5. Register ResourceBundleLoader for global language key resolution

`LDFResourceBundleLoader.java` registers as an OSGi `@Component(service = ResourceBundleLoader.class)` with properties:

- `bundle.symbolic.name=liferay.dummy.factory`
- `resource.bundle.base.name=content.Language`
- `servlet.context.name=liferay-dummy-factory`

`bnd.bnd` declares a `Provide-Capability: liferay.resource.bundle` header with matching attributes. This allows Liferay's `LanguageResourcesExtender` to discover and merge the module's `Language.properties` into the global resolution path, so that `LanguageUtil.process()` can replace string-literal `Liferay.Language.get('module-key')` calls at serve-time.

## Consequences

### Positive

- Build time is sub-second for incremental changes (esbuild), vs multi-second Webpack builds
- The build script is fully visible and debuggable — no hidden plugin chains
- Output format is identical to what Liferay's AMD loader expects
- Aligned with Liferay's own migration direction (npm-scripts → node-scripts/esbuild)

### Negative

- **No content-addressed filenames** — output is `__liferay__/index.js` (no hash). Browser caching requires cache-busting via query strings or short TTLs. Acceptable for a development/admin tool
- **Dual i18n resolution path** — String-literal `Liferay.Language.get('key')` calls are replaced at serve-time by `LanguageUtil.process()`, but variable-parameter calls require runtime resolution via JSP-injected `Liferay.Language._cache`. Developers must understand which mechanism applies to their call site. All keys are injected into the cache at page load regardless of whether they are used
- **Manual externalization** — only `react` and `react-dom` are externalized. If the portlet imports other portal-global packages in the future, the plugin must be updated manually
- **No watch mode / HMR** — rebuild requires `./gradlew :modules:liferay-dummy-factory:jar` + container deploy. Acceptable for a tool with infrequent frontend changes
- **Maintenance burden** — the ~165-line build script must track changes to Liferay's AMD bridge format, manifest schema, or module URL conventions across portal upgrades

### Lessons Learned

- `LanguageUtil.process()` replaces `Liferay.Language.get('literal')` at serve-time. Variable parameters (`Liferay.Language.get(variable)`) are not matched by the regex and survive to runtime
- Module-specific keys require a `ResourceBundleLoader` (e.g. `LDFResourceBundleLoader.java`) plus a `Provide-Capability: liferay.resource.bundle` header in `bnd.bnd` for serve-time replacement. Without this registration, string-literal calls resolve to the key itself (the standard Liferay fallback)
- JSP injection via `portletConfig.getResourceBundle(locale)` into `Liferay.Language._cache` provides runtime resolution for variable-parameter calls that `LanguageUtil.process()` cannot match
- Never hardcode module-specific i18n keys as string literals in `Liferay.Language.get()` without first confirming the `ResourceBundleLoader` is registered — if the key is not in the global resolution path, the serve-time replacement silently substitutes the key itself, and debugging the failure requires understanding both resolution mechanisms

## References

- Custom build script: `modules/liferay-dummy-factory/scripts/build.mjs`
- AMD bridge format: compare with portal's `modules/frontend-sdk/node-scripts/util/amd/writeMainBridge.mjs`
- ResourceBundleLoader: `modules/liferay-dummy-factory/src/main/java/com/liferay/support/tools/portlet/LDFResourceBundleLoader.java`
- bnd.bnd Provide-Capability header: `modules/liferay-dummy-factory/bnd.bnd`
- Language injection: `modules/liferay-dummy-factory/src/main/resources/META-INF/resources/view.jsp` (lines 20-36)
- Frontend i18n architecture: `.claude/rules/writing-code.md` § "Frontend i18n loading: JSP-injected ResourceBundle"
- ADR-0002: Use javax.portlet (3.0) for the Portlet API (related CE 7.4 compatibility decision)
