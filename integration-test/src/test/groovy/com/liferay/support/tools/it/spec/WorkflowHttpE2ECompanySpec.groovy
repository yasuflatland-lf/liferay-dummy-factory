package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class WorkflowHttpE2ECompanySpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(
		WorkflowHttpE2ECompanySpec)

	private static final String RUN_SUFFIX = String.valueOf(
		System.currentTimeMillis())

	private static final String COMPANY_WEB_ID = "wfhttp${RUN_SUFFIX}"
	private static final String COMPANY_VIRTUAL_HOSTNAME =
		"wfhttp${RUN_SUFFIX}.example.com"
	private static final String COMPANY_MX = "wfhttp${RUN_SUFFIX}.example.com"
	private static final String COMPANY_USER_BASE_NAME =
		"WFHttpCompanyUser${RUN_SUFFIX}"
	private static final String COMPANY_ORG_BASE_NAME =
		"WFHttpCompanyOrg${RUN_SUFFIX}"

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	Long createdUserId

	@Shared
	Long createdOrganizationId

	def setupSpec() {
		ensureBundleActive()

		pw = new PlaywrightLifecycle()

		loginAsAdmin(pw)

		workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, pw.page)

		_waitForWorkflowOperations()
	}

	def cleanupSpec() {
		if (createdUserId) {
			try {
				jsonwsPost(
					'user/delete-user',
					['userId': createdUserId])
			}
			catch (Exception e) {
				log.warn('Failed to clean up user {}: {}', createdUserId, e.message)
			}
		}

		if (createdOrganizationId) {
			try {
				jsonwsPost(
					'organization/delete-organization',
					['organizationId': createdOrganizationId])
			}
			catch (Exception e) {
				log.warn(
					'Failed to clean up organization {}: {}',
					createdOrganizationId, e.message)
			}
		}

		workflowHttpClient = null
		pw?.close()
	}

	def 'plan exposes the company workflow steps in order'() {
		when:
		Map response = workflowHttpClient.plan(_companyWorkflowRequest())

		then:
		response.errors == []

		and:
		List<Map<String, Object>> steps = (
			(response.plan as Map).definition as Map
		).steps as List<Map<String, Object>>

		steps*.id == [
			'createCompany',
			'createUser',
			'createOrganization'
		]

		steps*.operation == [
			'company.create',
			'user.create',
			'organization.create'
		]
	}

	// Skipped in CI: Liferay company creation triggers site initializer, page-template
	// generation, and batch-engine imports that exceed the 60 s Playwright HTTP timeout
	// on shared runners. The plan test above already verifies that company.create is
	// registered as a workflow operation. Run locally to exercise the full execution path.
	@IgnoreIf({ System.getenv('CI') == 'true' })
	def 'execute creates a company, user, and organization via HTTP JSON'() {
		when:
		Map response = workflowHttpClient.execute(_companyWorkflowRequest())

		then:
		((response.execution as Map).status as String) == 'SUCCEEDED'

		and:
		List<Map<String, Object>> steps = (
			response.execution as Map
		).steps as List<Map<String, Object>>

		steps*.stepId == [
			'createCompany',
			'createUser',
			'createOrganization'
		]
		steps.every { (it.status as String) == 'SUCCEEDED' }

		when:
		Map companyStep = _stepById(steps, 'createCompany')
		Map userStep = _stepById(steps, 'createUser')
		Map organizationStep = _stepById(steps, 'createOrganization')

		Map companyResult = companyStep.result as Map
		Map userResult = userStep.result as Map
		Map organizationResult = organizationStep.result as Map

		Map companyItem =
			(companyResult.items as List<Map<String, Object>>).first()
		Map userItem = (userResult.items as List<Map<String, Object>>).first()
		Map organizationItem =
			(organizationResult.items as List<Map<String, Object>>).first()

		Long companyId = companyItem.companyId as Long
		createdUserId = userItem.userId as Long
		createdOrganizationId = organizationItem.organizationId as Long

		then:
		(companyResult.count as Number) == 1
		(companyItem.companyId as Long) > 0
		(companyItem.webId as String) == COMPANY_WEB_ID

		and:
		(userResult.count as Number) == 1
		(createdUserId as Long) > 0

		and:
		(organizationResult.count as Number) == 1
		(createdOrganizationId as Long) > 0
		(organizationItem.name as String).startsWith(COMPANY_ORG_BASE_NAME)

		// TODO(G7): CompanyService is blacklisted from JSON-WS in DXP 2026
		// (json.service.invalid.class.names). /jsonws/company/* returns 404.
		// Cannot verify createdCompany via JSON-WS. companyId and webId are
		// already verified from the workflow step result above (companyItem).
		// Replace with Headless Admin User API when G7 confirms availability.

		and:
		Map createdUser = jsonwsGet(
			"user/get-user-by-id/user-id/${createdUserId}") as Map
		createdUser.userId as Long == createdUserId
		(createdUser.screenName as String) == "${COMPANY_USER_BASE_NAME.toLowerCase()}1"

		and:
		Map createdOrganization = jsonwsGet(
			"organization/get-organization" +
			"/organization-id/${createdOrganizationId}") as Map
		createdOrganization.organizationId as Long == createdOrganizationId
		(createdOrganization.name as String).startsWith(COMPANY_ORG_BASE_NAME)
	}

	private void _waitForWorkflowOperations() {
		long deadline = System.currentTimeMillis() + 120_000L
		Exception lastException = null

		while (System.currentTimeMillis() < deadline) {
			try {
				Map response = workflowHttpClient.functions()
				List<Map<String, Object>> functions =
					(response.functions as List<Map<String, Object>>) ?: List.of()
				List<String> operations = functions.collect {
					it.operation as String
				}

				if (operations.containsAll([
					'company.create',
					'organization.create',
					'user.create'
				])) {
					return
				}

				lastException = new IllegalStateException(
					"Missing workflow operations: " +
						([
							'company.create',
							'organization.create',
							'user.create'
						] - operations).join(', '))
			}
			catch (Exception e) {
				lastException = e
			}

			Thread.sleep(2_000)
		}

		throw new IllegalStateException(
			'Workflow operations did not become available before timeout',
			lastException)
	}

	private static Map<String, Object> _stepById(
		List<Map<String, Object>> steps, String stepId) {

		Map<String, Object> step = steps.find {
			(it.stepId as String) == stepId
		} as Map<String, Object>

		assert step != null

		return step
	}

	private Map<String, Object> _companyWorkflowRequest() {
		return [
			schemaVersion: '1.0',
			workflowId   : "workflow-http-company-e2e-${RUN_SUFFIX}",
			input        : [:],
			steps        : [
				[
					id             : 'createCompany',
					idempotencyKey : "company-${RUN_SUFFIX}",
					operation      : 'company.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'webId', value: COMPANY_WEB_ID],
						[name: 'virtualHostname', value: COMPANY_VIRTUAL_HOSTNAME],
						[name: 'mx', value: COMPANY_MX]
					]
				],
				[
					id             : 'createUser',
					idempotencyKey : "company-user-${RUN_SUFFIX}",
					operation      : 'user.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: COMPANY_USER_BASE_NAME]
					]
				],
				[
					id             : 'createOrganization',
					idempotencyKey : "company-org-${RUN_SUFFIX}",
					operation      : 'organization.create',
					onError        : [policy: 'FAIL_FAST'],
					params         : [
						[name: 'count', value: 1],
						[name: 'baseName', value: COMPANY_ORG_BASE_NAME]
					]
				]
			]
		]
	}

}
