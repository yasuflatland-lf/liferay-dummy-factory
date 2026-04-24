# Debugging

L2 layer for bug investigation, error analysis, and root-cause work on test failures.

## Posture

- Identify the **root cause**, not the symptom. Don't paper over with fallbacks or skipped checks.
- If you encounter unexpected files / branches / config, investigate before deleting or overwriting — it may be in-progress work.
- Don't bypass safety checks (`--no-verify`, etc.). If a hook fails, fix the underlying issue.

## Inspecting the Liferay container

### Hard reset: remove container and volume

```bash
./gradlew removeDockerContainer
```

Use this when the container is in an inconsistent state (failed startup, corrupt volume) and
`stopDockerContainer` alone does not resolve it. The next `startDockerContainer` will build
a fresh container from the image.

### Check bundle state via GoGo Shell

```bash
docker exec liferay bash -c "(echo 'lb dummy.factory'; sleep 2) | telnet localhost 11311"
```

`Active` / `ACTIVE` means the bundle is deployed. `UNSATISFIED` means a dependency failed to resolve.

### Check OSGi component state with `scr:info <component-name>`

Look for `UNSATISFIED REFERENCE` entries. For example, if the PortletTracker isn't picking up your portlet, the corresponding PanelApp `@Reference` will stay unsatisfied.

The PortletTracker / `jakarta.portlet` migration details are in `docs/details/api-liferay-dxp2026.md` (and `docs/ADR/adr-0008-dxp-2026-migration.md` records the migration decision).

### Check `Import-Package` with `headers <bundle-id>`

Verifies which portlet API package the bundle actually imports (should be `jakarta.portlet` for DXP 2026).

## Inspecting entity state via JSONWS

Skip the UI — query the database state directly via JSONWS:

```bash
curl -u test@liferay.com:test \
  "http://localhost:<httpPort>/api/jsonws/user/get-user-by-email-address?companyId=20117&emailAddress=test@liferay.com"
```

### JSONWS limits

- Liferay exposes remote `*Service` classes via `/api/jsonws/`, NOT `*LocalService`. If a method only exists on `*LocalService`, it cannot be called from a test or curl.
- Some remote services are blacklisted via `portal.properties` `json.service.invalid.class.names`. `CompanyServiceUtil` is one such entry — every path under `/api/jsonws/company/*` returns HTTP 404.

Before writing cleanup or verification code for a new entity type, check both: (a) does a remote `*Service` class with the method I need exist, and (b) is it blacklisted?

## Debugging test failures

### Groovy compile task name is `compileTestGroovy`, not `compileGroovy`

The integration-test project's sources live under `src/test/groovy/...`, so the production compile task `:integration-test:compileGroovy` reports `NO-SOURCE` and exits without checking anything. The task that actually compiles the specs is `:integration-test:compileTestGroovy`. When verifying "do the specs still compile after my edit?" without running the Liferay container, use `compileTestGroovy`; `compileGroovy` gives a false green.

### Gradle Incremental Build Trap (tests appear green but didn't actually run)

`:integration-test:integrationTest` does NOT declare `package.json` as an input. Changing a JS dependency does not invalidate the task, so Gradle marks it `UP-TO-DATE` and **replays the previous run's result**. A regression in the JS toolchain shows up as a green build.

Tell real runs from cached replays by the elapsed time on the `BUILD SUCCESSFUL in Xs` line. A real run is minutes, a cached replay is single-digit seconds.

For any change touching the frontend toolchain, the only trustworthy verification is:

```bash
./gradlew :modules:liferay-dummy-factory:clean :integration-test:clean
./gradlew :integration-test:integrationTest
```

Full Gradle execution details in `docs/details/testing-gradle.md`.

### Common Playwright failure modes

- **Tautology selector**: waiting only on `[data-testid="<entity>-result"]` passes on failure too. AND `.alert-success`. See the Playwright Success Assertion Pattern in `.claude/rules/testing.md`.
- **Visible-text selector**: `:has-text("create-user")` passes even when the i18n key is missing because the DOM literally contains the key string. See Liferay.Language Fallback in `.claude/rules/writing-code.md` and the Vitest i18n Guard in `.claude/rules/testing.md`.
- **`<option>` visible state**: collapsed `<select>` options are not "visible" — use `WaitForSelectorState.ATTACHED`. Detail in `docs/details/testing-playwright.md`.
- **`:has-text` vs `:text-is`**: substring vs exact. Strict-mode violation when `categories` matches both `categories` and `mb-categories`. Detail in `docs/details/testing-playwright.md`.
- **Headless Delivery `?search=`**: hits Elasticsearch with ingestion lag. For post-condition assertions, drop `?search=` and fetch with `?pageSize=100` then filter client-side.

### JSONWS 403 immediately after container start

If every JSONWS call returns 403 from the first test onwards, check whether
`configs/common/portal-liferay-online-config.properties` is deployed as an empty file:

```bash
docker exec <container-name> cat /home/liferay/portal-liferay-online-config.properties
```

If the output contains `json.servlet.hosts.allowed=N/A`, the shadow file was not deployed.
Verify that `configs/common/portal-liferay-online-config.properties` exists and is empty in
the repo, then rebuild the image with `./gradlew removeDockerContainer startDockerContainer`.
Detail: `docs/details/dxp-2026-gotchas.md` §1.

### Vitest migration gotchas

Silent skips and silently disabled type checking are easy to introduce. Mock typing, globals setting, RTL cleanup registration, `vi.mock` factory hoisting, React double-resolution, ESM `setup.ts` details. Full catalog in `docs/details/ui-vitest-gotchas.md`.

## Reading logs

- `./gradlew :integration-test:integrationTest --info` for verbose Gradle output.
- `docker logs -f <project-name>-liferay` for the container log (e.g. `docker logs -f liferay-dummy-factory-liferay`). Startup takes 5–8 minutes.
- Test logging includes `passed`, `skipped`, `failed`, `standardOut`, `standardError`.

## Known DXP 2026 API constraints

DXP 2026.Q1.3-LTS has several non-obvious API constraints (`GroupLocalService.addGroup` new 18-arg signature,
`CompanyService` blacklist, `MBThreadLocalService.getThreads` exact-match categoryId, `group/delete-group`
returning 404, `bnd.bnd` javax.servlet exclusion, etc.). Full catalog in `docs/details/api-liferay-dxp2026.md`.
