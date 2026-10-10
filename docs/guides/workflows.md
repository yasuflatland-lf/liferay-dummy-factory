# Create related data with workflows

A **workflow** is a JSON document that runs several Dummy Factory operations in order, feeding the ids created by one step into the next. One run can create a site, add pages to it and publish web content there — no copying ids between forms.

This guide walks through a first workflow. The complete contract is in the [Workflow API reference](../reference/workflow-api.md).

## Why use a workflow

- **One submit, whole scenario.** Company → users → organization, or site → pages → web content.
- **Ids flow automatically.** A later step reads `steps.<stepId>.items[0].groupId` instead of you looking it up.
- **Validate before you run.** *Plan* checks the whole workflow without creating anything.
- **Reproducible.** A workflow is plain JSON: commit it, share it, rerun it. The samples shipped in the UI are the same files the integration tests run.

## Your first workflow

Open **Liferay Dummy Factory → Workflow JSON**, pick the `site-and-page` sample and click **Load sample**. You get:

```json
{
  "schemaVersion": "1.0",
  "workflowId": "sample-site-and-page",
  "input": {},
  "steps": [
    {
      "id": "createSite",
      "operation": "site.create",
      "idempotencyKey": "sample-site-1",
      "onError": {"policy": "FAIL_FAST"},
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "sample-site"}
      ]
    },
    {
      "id": "createPage",
      "operation": "layout.create",
      "idempotencyKey": "sample-page-1",
      "onError": {"policy": "FAIL_FAST"},
      "params": [
        {"name": "count", "value": 1},
        {"name": "baseName", "value": "welcome"},
        {"name": "groupId", "from": "steps.createSite.items[0].groupId"},
        {"name": "type", "value": "portlet"}
      ]
    }
  ]
}
```

What it says:

- Two **steps** run top to bottom. Each has a unique `id`, an `operation`, and an `idempotencyKey`.
- Each parameter has either a literal **`value`** or a **`from`** reference — never both.
- `"from": "steps.createSite.items[0].groupId"` reads the `groupId` of the first site created by step `createSite`.

Click **Plan** to validate it, then **Execute** to run it. The result pane shows a summary per step; expand the details to see the full response, including the created ids.

## Writing your own

1. Find the operation and its parameters: the editor validates against the server's schema, and `GET /o/ldf-workflow/functions` lists every operation with its parameters, types and defaults.
2. Site-scoped operations (pages, blogs, documents, taxonomy, message boards) need a real site: chain a `site.create` step and reference its `groupId`. `groupId: 0` is rejected.
3. Plan, fix what it reports, then execute.

If a step fails, execution stops there (`FAIL_FAST`); entities created by earlier steps remain.

## Calling the API from a script

The same JSON can be posted directly, e.g. with Basic Auth on a local instance:

```bash
curl -u test@liferay.com:<password> \
  -H 'Content-Type: application/json' \
  -d @integration-test/src/test/resources/workflow-samples/site-and-page.json \
  http://localhost:8080/o/ldf-workflow/execute
```

Basic Auth must be enabled for `/o/*` in your portal; the development container is configured for it ([details](../reference/dxp-runtime-config.md)).

## More samples

The UI's sample picker contains every file in `integration-test/src/test/resources/workflow-samples/`: site and page, company/user/organization, vocabulary and category, role, documents, blogs and web content, and a four-step message-board chain. They are [listed in the reference](../reference/workflow-api.md#bundled-samples).
