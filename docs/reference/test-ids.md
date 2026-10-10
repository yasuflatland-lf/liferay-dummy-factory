# `data-testid` contract

The ids Playwright uses to find elements. Most are generated mechanically; specs must use the generated value and never invent a synonym. Rationale: [ADR-0003](../adr/0003-playwright-selector-strategy.md).

## Entity forms (generated)

`EntityForm.tsx` derives everything from the entity type:

```
entityKey = config.entityType.toLowerCase().replace(/_/g, '-')     ORG → org, MB_THREAD → mb-thread, USERS → users
field id  = `${entityKey}-${kebab(field.name)}-${suffix}`
```

| `field.type` | suffix |
|---|---|
| `text`, `number` | `input` |
| `select`, `multiselect` | `select` |
| `textarea` | `textarea` |
| `file` | `file` |
| `toggle` | `toggle` |

Fixed ids per form: `${entityKey}-submit`, `${entityKey}-result`, `${entityKey}-progress`.

Examples: `org-count-input`, `users-email-domain-input`, `mb-thread-format-select`, `org-submit`, `org-result`. Adding `maxUsers: number` to `ORG` yields `org-max-users-input`.

Entity-type values come from `ENTITY_TYPES` in `js/config/constants.ts` (`ORG`, `ROLES`, `USERS`, `WCM`, `DOC`, `PAGES`, `SITES`, `CATEGORY`, `VOCABULARY`, `COMPANY`, `MB_CATEGORY`, `MB_THREAD`, `MB_REPLY`, `BLOGS`). Use the derived key: `org`, not `organization`.

## Entity selector (exception)

Entity tabs use the **raw** enum value: `entity-selector-${entityType}` → `entity-selector-MB_THREAD`. This is the one place with upper-snake ids.

## App shell and Workflow JSON tab

| Id | Element |
|---|---|
| `app-tab-create-entities` | "Create Entities" tab |
| `app-tab-workflow-json` | "Workflow JSON" tab |
| `workflow-json-toolbar` | `role="toolbar"` holding Plan and Execute |
| `workflow-json-plan`, `workflow-json-execute` | Action buttons (disabled while the schema loads or fails, while busy, or without a URL) |
| `workflow-json-sample-select` | Sample picker |
| `workflow-json-load-sample` | Loads the selected sample into the editor |
| `workflow-json-textarea` | Editor |
| `workflow-json-progress` | Decorative progress bar (`aria-hidden`; wait with `ATTACHED`) |
| `workflow-json-result-placeholder` | Empty state before any result |
| `workflow-json-result-panel` | Result region (`role="status"`); AND `.alert-success` / `.alert-danger` |
| `workflow-json-result-source` | Source badge (`Ajv` / `Plan` / `Execute` / `Load`, CSS-uppercased — read with `textContent()`) |
| `workflow-json-result-title`, `-summary`, `-errors`, `-body` | Parts of the result |
| `workflow-json-result-toggle-details` | Expands the full response |

## Authoring rules

- Format: kebab-case, domain term first, role last (`user-count-input`). Not positional (`btn1`) or BEM (`Form__submit`).
- Only on elements a spec interacts with or asserts on: inputs, buttons, tabs, result/alert regions. Not on links, icons or decorative wrappers.
- Reusable components (`FormField`, `DynamicSelect`, `ResultAlert`, `ProgressBar`, `FileUploadArea`) take an optional `testId` prop and render `data-testid` only when it is given; the parent supplies the concrete id.
- Renaming or adding a generated id is a contract change: update the specs that use it in the same change.
