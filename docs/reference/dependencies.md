# Dependency reference

Where every version is pinned, and the constraints between pins that tooling cannot see. Versions themselves are **not** repeated in documentation — read them from the files below.

## Where versions live

| What | File |
|---|---|
| Liferay product, target platform (and therefore the Docker image and `release.dxp.api`) | `gradle.properties` (`liferay.workspace.product`, `liferay.workspace.target.platform.version`) |
| Liferay Workspace Gradle plugin | `settings.gradle` |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` |
| Spock, Groovy, Playwright (Java) | `gradle.properties` (`test.*`) |
| Other Java libraries (Datafaker, JUnit, JaCoCo, log4j, logback, commons-net) | `modules/liferay-dummy-factory/build.gradle`, `integration-test/build.gradle` |
| Java, Node, Yarn for local shells | `mise.toml` |
| Java / Node in CI | `.github/workflows/*.yml` |
| JS dependencies | `modules/liferay-dummy-factory/package.json` (+ root `package.json` `resolutions`), locked in `yarn.lock` |
| Automated update policy | `renovate.json` |

Java levels: the bundle compiles for Java 17 (`modules/liferay-dummy-factory/build.gradle`); the integration tests and the build itself run on JDK 21.

## Package manager

Yarn only. `yarn.lock` is the lockfile; `package-lock.json` is git-ignored and must never be committed. Never hand-edit `yarn.lock`: resolve a conflict by taking one side wholesale, then run `yarn install` and the tests.

## Renovate guard-rails

`renovate.json` automerges minor/patch updates after a 7-day minimum release age, and holds these back on purpose (each rule carries its reason):

| Package | Held at | Reason |
|---|---|---|
| Gradle wrapper | `< 8.6` | Newer Gradle breaks the Liferay Gradle plugins |
| `typescript` | `< 7` | TypeScript 7 (native compiler) not adopted yet |
| Groovy | `< 6` | The pinned Spock build does not compile on Groovy 6 |
| Liferay Workspace plugin | no automerge | Must be bumped together with the target platform |

## Constraints between pins

### Exact versions for the Node-sensitive toolchain

`vite`, `vitest`, `@vitest/coverage-v8`, `@vitejs/plugin-react`, `jsdom`, `esbuild` and `typescript` are pinned **exactly** (no `^`/`~`). A caret range silently resolves to a newer minor whose Node engine floor may exceed the CI Node version. Engine mismatches do not necessarily fail installation, so they surface later — e.g. jsdom 30 on Node 20 killed every Vitest worker with `webidl.util.markAsUncloneable is not a function`. Treat any failure right after a bump as a pinning regression first. Origin of the rule: [ADR-0005](../adr/0005-node-vitest-version-pinning.md).

### Lockstep pairs

| Pair | Why |
|---|---|
| `vitest` ↔ `@vitest/coverage-v8` | Exact peer dependency; a lone `vitest` bump installs with only a warning and then `vitest run --coverage` fails in CI |
| module `vite` ↔ root `package.json` `resolutions.vite` | The resolution collapses every transitive `vite` request onto one copy. A nested second copy once pulled in a third `esbuild` line whose post-install check failed (`Expected "0.27.7" but got "0.28.1"`) |
| `@types/react` ↔ `@types/react-dom` | Use the highest patch published for **both**; skew silently corrupts types such as `ReactNode` |
| `react` ↔ `react-dom` | Same version |
| Playwright Java (`test.playwright.version`) ↔ Maven Central | Confirm the POM exists before bumping ([how](playwright.md#basics)) |

### TypeScript

TypeScript 6 rejects `moduleResolution: "node"` and `baseUrl`; `tsconfig.json` uses `moduleResolution: "bundler"` without `baseUrl`.

### Known harmless warning

`@liferay/npm-scripts` (kept for formatting) carries its own `@testing-library/react@14` with a React 18 peer, so installs warn about an incorrect React peer. The module's own Testing Library version wins for Vitest. Do not "fix" it with downgrades or `resolutions`.

## Upgrading the Liferay target

1. Change `liferay.workspace.product` and `liferay.workspace.target.platform.version` in `gradle.properties` together. Check whether the Workspace plugin in `settings.gradle` needs a matching bump.
2. `./gradlew :modules:liferay-dummy-factory:jar` — signature changes appear as compile errors; record each one in [`liferay-dxp-api.md`](liferay-dxp-api.md).
3. `./gradlew removeDockerContainer` and run the full integration suite against the new image.
4. Search the docs for the old version string and update the [compatibility table](../../README.md#compatibility).
