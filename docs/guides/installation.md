# Install and use Dummy Factory

For people who want to generate test data in their own Liferay. Dummy Factory is a development tool: never install it on a production system.

## 1. Pick the JAR for your Liferay version

Each Liferay version has its own branch with a prebuilt JAR in `latest/`. Use the [compatibility table in the README](../../README.md#compatibility).

## 2. Deploy it

1. Copy the JAR into `${LIFERAY_HOME}/deploy/`.
2. Watch the log for `STARTED liferay.dummy.factory_…`.
3. Optional check from the GoGo shell (`telnet localhost 11311`): `lb dummy.factory` should show the bundle as `Active`.

No portal restart and no extra configuration are needed.

## 3. Open it

Sign in as an administrator, open the **Applications Menu → Control Panel**, and choose **Liferay Dummy Factory** (it is registered in the Control Panel's apps category). The direct URL is:

```
http://<host>/group/control_panel/manage?p_p_id=com_liferay_support_tools_portlet_LiferayDummyFactoryPortlet
```

## 4. Create data

The portlet has two tabs:

- **Create Entities** — pick an entity type, fill the form (count, base name, and entity-specific options), and submit. A progress bar tracks long batches, and the result shows how many entities were created.
- **Workflow JSON** — create several related entities in one run (for example a site, its pages and its web content). See [Workflows](workflows.md).

Entities you can create:

| Entity | Notes |
|---|---|
| Organizations | optionally with an organization site |
| Roles | regular, site, organization and other role types |
| Users | fixed or Datafaker-generated names, optional site/organization/role membership |
| Sites | optionally from a site template |
| Pages | several page types, public or private |
| Web content | in one or more sites, with optional tags |
| Documents | from uploaded template files, with optional tags |
| Blogs | blog entries in a site |
| Vocabularies and categories | |
| Message boards | categories, threads and replies, with optional tags |
| Companies | virtual instances |

### Rules that may surprise you

- **A batch succeeds only if every entity is created.** Asking for 10 and getting 9 is reported as a failure, with the 9 created entities listed and the error explained. Already-created entities are kept.
- **Names you type are validated, not rewritten.** If a base name contains characters Liferay does not allow (for example in a user screen name), the request is rejected with a message instead of being silently changed.
