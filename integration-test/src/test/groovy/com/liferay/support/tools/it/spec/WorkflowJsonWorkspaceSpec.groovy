package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.Response

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class WorkflowJsonWorkspaceSpec extends BaseLiferaySpec {

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	private static final String WORKFLOW_JSON_EDITOR_TEST_ID =
		'workflow-json-textarea'
	private static final String WORKFLOW_JSON_PLAN_TEST_ID =
		'workflow-json-plan'
	private static final String WORKFLOW_JSON_EXECUTE_TEST_ID =
		'workflow-json-execute'
	private static final String WORKFLOW_JSON_RESULT_TITLE_TEST_ID =
		'workflow-json-result-title'
	private static final String WORKFLOW_JSON_RESULT_BODY_TEST_ID =
		'workflow-json-result-body'
	private static final String USERS_SELECTOR_TEST_ID = 'entity-selector-USERS'
	private static final String USERS_COUNT_INPUT_TEST_ID = 'users-count-input'
	private static final String USERS_BASE_NAME_INPUT_TEST_ID =
		'users-base-name-input'
	private static final String USERS_SUBMIT_TEST_ID = 'users-submit'
	private static final String USERS_RESULT_TEST_ID = 'users-result'
	private static final List<String> WORKFLOW_JSON_ENTRY_SELECTORS = [
		'[data-testid="app-tab-workflow-json"]',
		'[data-testid="workflow-json-tab"]',
		'[data-testid="workspace-tab-WORKFLOW_JSON"]',
		'[data-testid="app-tab-WORKFLOW_JSON"]',
		'[role="tab"]:has-text("Workflow JSON")',
		'[data-testid="entity-selector-WORKFLOW_JSON"]',
	]
	private static final List<String> OTHER_ENTITIES_ENTRY_SELECTORS = [
		'[data-testid="app-tab-create-entities"]',
		'[data-testid="other-entities-tab"]',
		'[data-testid="workspace-tab-OTHER_ENTITIES"]',
		'[data-testid="app-tab-OTHER_ENTITIES"]',
		'[role="tab"]:has-text("Other Entities")',
	]
	private static final List<String> WORKFLOW_JSON_LOAD_SAMPLE_SELECTORS = [
		'[data-testid="workflow-json-load-sample"]',
		'[data-testid="workflow-json-sample-load"]',
	]
	private static final List<String> WORKFLOW_JSON_RESULT_SUMMARY_SELECTORS = [
		'[data-testid="workflow-json-result-summary"]',
		'[data-testid="workflow-json-result-panel"]',
	]
	private static final List<String> WORKFLOW_JSON_RESULT_DETAIL_TOGGLE_SELECTORS = [
		'[data-testid="workflow-json-result-toggle-details"]',
		'[data-testid="workflow-json-result-details-toggle"]',
		'[data-testid="workflow-json-result-expand"]',
	]

	@Shared
	PlaywrightLifecycle pw

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)
	}

	def cleanupSpec() {
		pw?.close()
	}

	def 'Workflow JSON workspace tab runs plan and execute and shows result feedback'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON workspace tab is opened'
		_openWorkflowJsonWorkspace(page)

		and: 'the JSON editor is available'
		Locator editor = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		)
		editor.waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		and: 'a sample is loaded into the editor before any backend action'
		String initialEditorValue = editor.inputValue()

		_clickFirstVisible(page, WORKFLOW_JSON_LOAD_SAMPLE_SELECTORS)

		String editorJson = _waitForEditorJson(editor, initialEditorValue)
		Map<String, Object> loadedWorkflowRequest = _normalizeWorkflowRequest(
			new JsonSlurper().parseText(editorJson) as Map<String, Object>)

		editor.fill(JsonOutput.prettyPrint(JsonOutput.toJson(loadedWorkflowRequest)))
		String normalizedEditorJson = editor.inputValue()

		then: 'the loaded sample is valid workflow JSON'
		(normalizedEditorJson as String).contains('"schemaVersion"')
		(loadedWorkflowRequest.schemaVersion as String) == '1.0'
		((loadedWorkflowRequest.steps ?: []) as List).size() > 0

		when: 'planning is triggered from the workspace'
		String previousResultTitle = _readResultTitle(page)
		Response planResponse = _runWorkflowAction(
			page, WORKFLOW_JSON_PLAN_TEST_ID, ['/o/ldf-workflow/plan'])

		Map<String, Object> planResponseBody = new JsonSlurper().parseText(
			planResponse.text()) as Map<String, Object>

		then: 'the plan endpoint returns a rendered workflow plan'
		planResponse.status() == 200
		(planResponseBody.errors ?: []) == []
		(planResponseBody.plan as Map<String, Object>) != null
		(((planResponseBody.plan as Map<String, Object>).definition as Map<String, Object>).steps as List).size() > 0
		page.locator(
			'[data-testid="workflow-json-result-panel"].alert-success'
		).waitFor(new Locator.WaitForOptions().setTimeout(15_000))
		_assertResultSummaryUpdated(page, previousResultTitle)
		_expandResultDetailsIfAvailable(page)

		when: 'execution is triggered from the workspace'
		previousResultTitle = _readResultTitle(page)
		Response executeResponse = _runWorkflowAction(
			page, WORKFLOW_JSON_EXECUTE_TEST_ID, ['/o/ldf-workflow/execute'])

		Map<String, Object> executionResponse = new JsonSlurper().parseText(
			executeResponse.text()) as Map<String, Object>

		then: 'the loaded sample executes successfully'
		executeResponse.status() == 200
		((executionResponse.execution as Map<String, Object>).status as String) ==
			'SUCCEEDED'
		(((executionResponse.execution as Map<String, Object>).steps ?: []) as List).size() > 0
		(((executionResponse.execution as Map<String, Object>).steps ?: []) as List<Map<String, Object>>).every {
			(it.status as String) == 'SUCCEEDED'
		}
		page.locator(
			'[data-testid="workflow-json-result-panel"].alert-success'
		).waitFor(new Locator.WaitForOptions().setTimeout(15_000))
		_assertResultSummaryUpdated(page, previousResultTitle)
		_expandResultDetailsIfAvailable(page)
	}

	def 'Legacy entity form still renders and submits after switching away from Workflow JSON'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON workspace is visited first'
		_openWorkflowJsonWorkspace(page)

		and: 'the legacy entities workspace is re-opened when tabbed UI is present'
		_clickFirstVisibleIfPresent(page, OTHER_ENTITIES_ENTRY_SELECTORS)

		and: 'the legacy Users form is selected'
		page.locator(
			"[data-testid=\"${USERS_SELECTOR_TEST_ID}\"]"
		).click()
		page.locator(
			"[data-testid=\"${USERS_COUNT_INPUT_TEST_ID}\"]"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		and: 'the users form is filled and submitted'
		page.locator("[data-testid=\"${USERS_COUNT_INPUT_TEST_ID}\"]").fill('1')
		page.locator("[data-testid=\"${USERS_BASE_NAME_INPUT_TEST_ID}\"]").fill(
			"WorkflowJsonLegacyUser${RUN_SUFFIX}"
		)

		Response response = page.waitForResponse(
			{ Response r -> r.url().contains('p_p_resource_id=%2Fldf%2Fuser') },
			{
				page.locator("[data-testid=\"${USERS_SUBMIT_TEST_ID}\"]").click()
			})

		then: 'the legacy form still submits successfully'
		response.status() == 200
		page.locator(
			"[data-testid=\"${USERS_RESULT_TEST_ID}\"].alert-success"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		page.locator(
			"[data-testid=\"${USERS_RESULT_TEST_ID}\"].alert-success"
		).isVisible()
	}

	def 'shows Ajv empty-variant result in the result pane when Execute is clicked with empty editor'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON workspace tab is opened'
		_openWorkflowJsonWorkspace(page)

		and: 'the textarea is cleared'
		Locator editor = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		)
		editor.waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		editor.fill('')

		and: 'Execute is clicked'
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EXECUTE_TEST_ID}\"]"
		).click(new Locator.ClickOptions().setForce(true))

		then: 'the result pane appears with an Ajv source badge'
		Locator resultPanel = page.locator(
			'[data-testid="workflow-json-result-panel"].alert-danger'
		)
		resultPanel.waitFor(new Locator.WaitForOptions().setTimeout(5_000))

		Locator sourceBadge = page.locator(
			'[data-testid="workflow-json-result-source"]'
		)

		assert sourceBadge.textContent() == 'Ajv'

		and: 'the result text is the resolved English phrase (not the key)'
		String resultText = resultPanel.textContent()

		assert resultText.contains('Workflow JSON is required')
		assert !resultText.contains('workflow-json-empty-error')
	}

	def 'shows progress bar while Execute is in-flight'() {
		given:
		Page page = pw.page

		when: 'the portlet is opened'
		_openPortlet(page)

		and: 'the Workflow JSON workspace tab is opened'
		_openWorkflowJsonWorkspace(page)

		and: 'a valid sample is loaded into the editor'
		Locator editor = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		)
		editor.waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		String initialEditorValue = editor.inputValue()

		_clickFirstVisible(page, WORKFLOW_JSON_LOAD_SAMPLE_SELECTORS)

		String editorJson = _waitForEditorJson(editor, initialEditorValue)

		// Normalize before executing so the in-flight workflow does not
		// create entities with raw sample names that collide with other
		// specs sharing the container (e.g. WorkflowSampleTemplateSpec).
		Map<String, Object> loadedWorkflowRequest = _normalizeWorkflowRequest(
			new JsonSlurper().parseText(editorJson) as Map<String, Object>)

		editor.fill(JsonOutput.prettyPrint(JsonOutput.toJson(loadedWorkflowRequest)))

		and: 'Execute is clicked once the schema is ready'
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EXECUTE_TEST_ID}\"]:not([disabled])"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EXECUTE_TEST_ID}\"]"
		).click()

		then: 'the progress bar becomes visible while the action is in-flight'
		page.locator('[data-testid="workflow-json-progress"]').waitFor(
			new Locator.WaitForOptions()
				.setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
				.setTimeout(5_000)
		)
	}

	private static void _openPortlet(Page page) {
		page.navigate(
			"${liferay.baseUrl}/group/control_panel/manage" +
			"?p_p_id=${PORTLET_ID}" +
			'&p_p_lifecycle=0' +
			'&p_p_state=maximized'
		)
		page.waitForLoadState()
	}

	private static void _openWorkflowJsonWorkspace(Page page) {
		_clickFirstVisible(page, WORKFLOW_JSON_ENTRY_SELECTORS)

		page.locator(
			"[data-testid=\"${WORKFLOW_JSON_EDITOR_TEST_ID}\"]"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))
	}

	private static Response _runWorkflowAction(
		Page page, String actionTestId, List<String> endpointPaths) {

		// Wait for schema to finish loading: the button is disabled while
		// schemaStatus === 'loading'. Separating this wait from waitForResponse
		// gives each phase its own 30-second budget.
		page.locator(
			"[data-testid=\"${actionTestId}\"]:not([disabled])"
		).waitFor(new Locator.WaitForOptions().setTimeout(30_000))

		return page.waitForResponse(
			{ Response response ->
				response.request().method() == 'POST' &&
					endpointPaths.any { String endpointPath ->
						response.url().contains(endpointPath)
					}
			},
			{
				page.locator(
					"[data-testid=\"${actionTestId}\"]"
				).click()
			}
		)
	}

	private static void _assertResultSummaryUpdated(
		Page page, String previousResultTitle) {

		_waitForAnyVisibleSelector(page, WORKFLOW_JSON_RESULT_SUMMARY_SELECTORS)

		String currentResultTitle = _readResultTitle(page)

		assert currentResultTitle

		if (previousResultTitle) {
			assert currentResultTitle != previousResultTitle
		}
	}

	private static String _readResultTitle(Page page) {
		Locator titleLocator = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_RESULT_TITLE_TEST_ID}\"]"
		)

		if ((titleLocator.count() > 0) && titleLocator.first().isVisible()) {
			return titleLocator.first().innerText()?.trim()
		}

		Locator summaryLocator = page.locator(
			'[data-testid="workflow-json-result-summary"]'
		)

		if ((summaryLocator.count() > 0) && summaryLocator.first().isVisible()) {
			return summaryLocator.first().innerText()?.trim()
		}

		return null
	}

	private static void _expandResultDetailsIfAvailable(Page page) {
		Locator toggleLocator = _findFirstVisibleLocator(
			page, WORKFLOW_JSON_RESULT_DETAIL_TOGGLE_SELECTORS)

		if (toggleLocator) {
			toggleLocator.click(new Locator.ClickOptions().setForce(true))
		}

		Locator bodyLocator = page.locator(
			"[data-testid=\"${WORKFLOW_JSON_RESULT_BODY_TEST_ID}\"]"
		)

		if (bodyLocator.count() > 0) {
			if (toggleLocator && !bodyLocator.first().isVisible()) {
				bodyLocator.first().waitFor(
					new Locator.WaitForOptions().setTimeout(10_000))
			}

			if (bodyLocator.first().isVisible()) {
				assert bodyLocator.first().innerText()?.trim()
			}
		}
	}

	private static void _clickFirstVisibleIfPresent(
		Page page, List<String> selectors) {

		Locator locator = _findFirstVisibleLocator(page, selectors)

		if (locator) {
			locator.click(new Locator.ClickOptions().setForce(true))
		}
	}

	private static void _clickFirstVisible(Page page, List<String> selectors) {
		Locator locator = _waitForAnyVisibleSelector(page, selectors)

		locator.click(new Locator.ClickOptions().setForce(true))
	}

	private static Locator _waitForAnyVisibleSelector(
		Page page, List<String> selectors) {

		long deadline = System.currentTimeMillis() + 30_000L

		while (System.currentTimeMillis() < deadline) {
			Locator locator = _findFirstVisibleLocator(page, selectors)

			if (locator) {
				return locator
			}

			Thread.sleep(200)
		}

		throw new IllegalStateException(
			"No visible selector found from: ${selectors.join(', ')}")
	}

	private static Locator _findFirstVisibleLocator(
		Page page, List<String> selectors) {

		for (String selector : selectors) {
			Locator locator = page.locator(selector)

			if (locator.count() <= 0) {
				continue
			}

			try {
				if (locator.first().isVisible()) {
					return locator.first()
				}
			}
			catch (Exception ignored) {
			}
		}

		return null
	}

	private static Map<String, Object> _normalizeWorkflowRequest(
		Map<String, Object> workflowRequest) {

		Map<String, Object> normalizedRequest = new LinkedHashMap<>(
			workflowRequest ?: [:]
		)

		normalizedRequest.workflowId =
			"workflow-json-workspace-it-${RUN_SUFFIX}"

		List<Map<String, Object>> normalizedSteps = []

		((workflowRequest.steps ?: []) as List<Map<String, Object>>).eachWithIndex {
			Map<String, Object> step, int index ->

			Map<String, Object> normalizedStep = new LinkedHashMap<>(step)

			normalizedStep.idempotencyKey =
				"${step.idempotencyKey ?: "workflow-json-step-${index}"}-${RUN_SUFFIX}"
			normalizedStep.params = _normalizeStepParams(
				(step.params ?: []) as List<Map<String, Object>>
			)

			normalizedSteps.add(normalizedStep)
		}

		normalizedRequest.steps = normalizedSteps

		return normalizedRequest
	}

	private static List<Map<String, Object>> _normalizeStepParams(
		List<Map<String, Object>> params) {

		return params.collect { Map<String, Object> param ->
			Map<String, Object> normalizedParam = new LinkedHashMap<>(param)
			String name = param.name as String

			if (name == 'count') {
				normalizedParam.value = 1
			}
			else if ([
				'baseName',
				'description',
				'mx',
				'name',
				'virtualHostname',
				'webId'
			].contains(name) && (param.value != null)) {
				normalizedParam.value = "${param.value}-${RUN_SUFFIX}"
			}

			return normalizedParam
		}
	}

	private static String _waitForEditorJson(
		Locator editor, String previousValue) {

		long deadline = System.currentTimeMillis() + 30_000L

		while (System.currentTimeMillis() < deadline) {
			String currentValue = editor.inputValue()

			if (currentValue?.trim() && (currentValue != previousValue)) {
				return currentValue
			}

			Thread.sleep(200)
		}

		throw new IllegalStateException(
			'Workflow JSON sample did not populate the editor before timeout')
	}

}
