# Releasing

How a build becomes the downloadable JAR, and how Liferay versions map to branches.

## Branch model

- `master` targets the Liferay version in `gradle.properties`.
- Every supported Liferay version has a branch named after it (`7.3.x`, `7.4.4`, `2024.q3.9`, `2026.Q1.9-LTS`, `2026.Q3.6`, …), each with its own `latest/` JAR. When `master` moves to a new Liferay version, cut a branch for the version it leaves behind.
- The README [compatibility table](../../README.md#compatibility) lists every branch. Keep it in sync when a branch is added.

## Cutting a release on a branch

1. Make sure CI is green on the commit (unit and integration).
2. If you change the bundle version, change `Bundle-Version` in `modules/liferay-dummy-factory/bnd.bnd` and `version` in `modules/liferay-dummy-factory/package.json` together.
3. Publish the JAR into `latest/`:

   ```bash
   ./gradlew :modules:liferay-dummy-factory:jar :modules:liferay-dummy-factory:copyJarToLatest
   ```

4. Commit `latest/liferay-dummy-factory.jar` in the release PR.

`copyJarToLatest` is deliberately **not** hooked to `jar` (the hook was removed so ordinary builds do not dirty `latest/`). Do not re-add it.

## Moving `master` to a new Liferay version

Follow [Upgrading the Liferay target](../reference/dependencies.md#upgrading-the-liferay-target), then release as above and add the new row to the compatibility table.
