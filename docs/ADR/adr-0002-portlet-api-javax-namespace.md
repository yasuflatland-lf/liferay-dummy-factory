# ADR-0002: Use javax.portlet (3.0) for the Portlet API

## Status

Accepted

## Date

2026-04-10

## Context

After deploying the portlet to the Docker image `liferay/portal:7.4.3.132-ga132`, the bundle was Active but the PanelApp's `@Reference(com.liferay.portal.kernel.model.Portlet)` remained UNSATISFIED, and the portlet did not appear in the Control Panel.

### Investigation Process

1. Checking via GoGo Shell revealed that the portlet's SCR component was SATISFIED and registered as a service under `jakarta.portlet.Portlet`
2. However, the `com.liferay.portal.kernel.model.Portlet` service that the PanelApp depends on was never created
3. Traced the `PortletTracker` in the Liferay source and identified the flow: `_addingPortlet()` -> `setReady(true)` -> `model.Portlet` OSGi service registration
4. None of the PortletTracker's log messages (`"Adding"`, `"Added"`, `"failed to initialize"`, `"already in use"`) were output -> `addingService()` itself was never called
5. **Critical discovery**: Inspecting the `Import-Package` of the PortletTracker inside the Docker container (bundle 27, `com.liferay.portal.osgi.web.portlet.tracker:6.0.39`) revealed that it imports `javax.portlet` (not `jakarta.portlet`)

### Root Cause

| Layer | Namespace in use | Portlet API version |
|-------|-----------------|---------------------|
| Build dependency (`release.dxp.api:2026.q1.2`) | `jakarta.portlet` | 4.0 |
| Docker image (`liferay/portal:7.4.3.132-ga132`) | `javax.portlet` | 3.0 |

`release.dxp.api:default` resolved to the DXP 2024+ API (`2026.q1.2`), which was incompatible with the CE 7.4 GA132 runtime. Since the PortletTracker's `ServiceTracker` tracks `javax.portlet.Portlet`, portlets registered as `jakarta.portlet.Portlet` could not be detected.

## Decision

### 1. Switch build dependency to the CE edition

```groovy
// Before (DXP 2024+ API — jakarta namespace)
compileOnly group: "com.liferay.portal", name: "release.dxp.api", version: "default"
// -> Resolves to 2026.q1.2

// After (CE 7.4 GA132 API — javax namespace)
compileOnly group: "com.liferay.portal", name: "release.portal.api", version: "default"
// -> Resolves to 7.4.3.132
```

### 2. Use `javax.portlet` in the portlet code

```java
// Before
import jakarta.portlet.Portlet;
// @Component property: "jakarta.portlet.name=...", "jakarta.portlet.version=4.0"

// After
import javax.portlet.Portlet;
// @Component property: "javax.portlet.name=...", "javax.portlet.version=3.0"
```

### 3. Align the PanelApp @Reference target accordingly

```java
// Before
@Reference(target = "(jakarta.portlet.name=" + ... + ")")

// After
@Reference(target = "(javax.portlet.name=" + ... + ")")
```

## Consequences

### Positive

- The build API and the runtime (Docker image) versions are aligned, so OSGi service tracking works correctly
- The PortletTracker detects the `javax.portlet.Portlet` service and registers `com.liferay.portal.kernel.model.Portlet`, which resolves the PanelApp's `@Reference`

### Negative

- The "use `jakarta.portlet`" rule in `.claude/rules/writing-code.md` does not apply to CE 7.4 GA132. This rule only becomes valid for DXP 2024+ / CE GA120+ once the corresponding `release.portal.api` provides the jakarta namespace

### Lessons Learned: Debugging Methodology

Debugging steps when the PortletTracker is unresponsive:

1. `scr:info <PanelApp FQCN>` — Check component state and UNSATISFIED REFERENCE entries
2. `scr:info <Portlet FQCN>` — Check the portlet component state
3. `services jakarta.portlet.Portlet` / `services javax.portlet.Portlet` — Verify the namespace of the service registration
4. `headers <PortletTracker bundle ID>` — Check whether PortletTracker's `Import-Package` uses `javax.portlet` or `jakarta.portlet`
5. `docker logs <container> 2>&1 | grep "failed to initialize"` — Check for PortletTracker initialization errors

## References

- Liferay PortletTracker source: `modules/apps/static/portal-osgi-web/portal-osgi-web-portlet-tracker/`
  - `PortletTracker.java` — `addingService()` (L119-213), `_addingPortlet()` (L363-473)
- `PortletImpl.java` — `setReady(true)` registers `com.liferay.portal.kernel.model.Portlet` as an OSGi service (L3725-3758)
- ADR-0001: Integration Test Architecture
