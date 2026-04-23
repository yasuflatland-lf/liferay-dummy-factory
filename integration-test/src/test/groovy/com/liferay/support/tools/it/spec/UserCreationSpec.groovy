package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.JsonwsSetupHelper
import com.liferay.support.tools.it.util.LdfResourceClient
import com.liferay.support.tools.it.util.PlaywrightLifecycle
import com.liferay.support.tools.it.util.WorkflowHttpClient

import spock.lang.Shared
import spock.lang.Stepwise

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Stepwise
class UserCreationSpec extends BaseLiferaySpec {

	private static final Logger log = LoggerFactory.getLogger(UserCreationSpec)

	private static final String RUN_SUFFIX =
		String.valueOf(System.currentTimeMillis())

	private static final String BASIC_BASE_NAME = "basicUser${RUN_SUFFIX}"
	private static final String FAKER_BASE_NAME = "fakerUser${RUN_SUFFIX}"
	private static final String ASSIGN_BASE_NAME = "assignUser${RUN_SUFFIX}"
	private static final String GROUP_BASE_NAME = "groupUser${RUN_SUFFIX}"
	private static final String LAYOUT_BASE_NAME = "layoutUser${RUN_SUFFIX}"
	private static final String PROTO_BASE_NAME = "protoUser${RUN_SUFFIX}"
	private static final String WF_LAYOUT_BASE_NAME = "wflayoutuser${RUN_SUFFIX}"

	@Shared
	LdfResourceClient ldf

	@Shared
	JsonwsSetupHelper jsonws

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	List<Long> createdUserIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient(liferay.baseUrl)

		// Log in the admin session so JsonwsSetupHelper (which uses Basic Auth against
		// JSONWS) runs under an already-primed cookie store. D2 removed the
		// password-change detour; this is now a one-shot login.
		ldf.login()

		jsonws = new JsonwsSetupHelper(liferay.baseUrl)

		pw = new PlaywrightLifecycle()
		loginAsAdmin(pw)
		workflowHttpClient = new WorkflowHttpClient(liferay.baseUrl, pw.page)
	}

	def cleanupSpec() {
		createdUserIds.each { id ->
			try {
				jsonwsPost(
					'user/delete-user',
					['userId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up user {}: {}', id, e.message)
			}
		}

		jsonws?.cleanupAll()
		ldf?.close()
		pw?.close()
	}

	def 'creates users with basic fields'() {
		given:
		int count = 3

		when: 'POST /ldf/user with basic fields'
		Map response = ldf.createUser([
			count   : count,
			baseName: BASIC_BASE_NAME
		])

		then: 'response reports success'
		response.success == true
		(response.items as List).size() == count

		and: 'response shape contract is complete'
		response.containsKey('success')
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')
		(response.requested as Integer) == count
		(response.skipped as Integer) == 0

		and: 'each user is discoverable via JSONWS get-user-by-screen-name'
		String prefix = BASIC_BASE_NAME.toLowerCase()

		(1..count).each { i ->
			String screenName = "${prefix}${i}"

			Map user = jsonwsGet(
				"user/get-user-by-screen-name" +
				"/company-id/${companyId}" +
				"/screen-name/${URLEncoder.encode(screenName, 'UTF-8')}"
			) as Map

			assert user != null
			assert (user.screenName as String) == screenName

			createdUserIds << (user.userId as Long)
		}
	}

	def 'creates users with Datafaker-generated names when fakerEnable and locale set'() {
		given:
		int count = 2

		when: 'POST /ldf/user with fakerEnable=true and locale=en_US'
		// en_US: ja_JP kanji names are rejected by Liferay's default screen-name validator
		Map response = ldf.createUser([
			count       : count,
			baseName    : FAKER_BASE_NAME,
			fakerEnable : true,
			locale      : 'en_US'
		])

		then: 'response reports success'
		response.success == true
		(response.items as List).size() == count

		when: 'fetch each generated user by screen name'
		List<Map> createdUsers = (response.items as List).collect { it as Map }

		List<Map> dbUsers = createdUsers.collect { Map created ->
			String screenName = created.screenName as String

			Map user = jsonwsGet(
				"user/get-user-by-screen-name" +
				"/company-id/${companyId}" +
				"/screen-name/${URLEncoder.encode(screenName, 'UTF-8')}"
			) as Map

			createdUserIds << (user.userId as Long)

			return user
		}

		then: 'screenName is Datafaker-derived, not the baseName+index fallback'
		String fallbackPrefix = FAKER_BASE_NAME.toLowerCase()

		dbUsers.every { Map u ->
			!(u.screenName as String).startsWith(fallbackPrefix)
		}

		and: 'firstName is Datafaker-derived, not the baseName fallback'
		dbUsers.every { Map u -> (u.firstName as String) != FAKER_BASE_NAME }

		and: 'all returned screen names match Liferay-legal characters'
		(response.items as List).every {
			(it.screenName as String) ==~ /^[a-z0-9._-]+$/
		}
	}

	def 'rejects non-faker baseName that contains invalid characters'() {
		when: 'a dirty baseName is submitted with faker disabled'
		Map response = ldf.createUser([
			count      : 1,
			baseName   : "O'Brien",
			fakerEnable: false
		])

		then: 'server rejects the request with a validation error'
		response.success == false
		response.error != null
		(response.error as String).contains("Invalid baseName")
	}

	def 'assigns users to organizations and roles'() {
		given:
		String orgName = "ITUserCreateOrg${RUN_SUFFIX}"
		String roleName = "ITUserCreateRole${RUN_SUFFIX}"

		Map org = jsonws.createOrganization(orgName)
		long orgId = org.organizationId as Long

		Map role = jsonws.createRole(roleName)
		long roleId = role.roleId as Long

		when: 'POST /ldf/user with organizationIds and roleIds'
		Map response = ldf.createUser([
			count          : 1,
			baseName       : ASSIGN_BASE_NAME,
			organizationIds: [orgId],
			roleIds        : [roleId]
		])

		then:
		response.success == true

		when:
		Map createdUser = (response.items as List).first() as Map
		long userId = createdUser.userId as Long
		createdUserIds << userId

		and: 'organization membership via JSONWS get-organization-users'
		List orgUsers = jsonwsGet(
			"user/get-organization-users" +
			"/organization-id/${orgId}"
		) as List

		then:
		orgUsers?.any { (it.userId as Long) == userId }

		when: 'role membership via JSONWS has-role-user'
		Object hasRole = jsonwsGet(
			"user/has-role-user" +
			"/role-id/${roleId}" +
			"/user-id/${userId}"
		)

		then:
		hasRole == true
	}

	def 'assigns users to sites via groupIds (regression for old port)'() {
		given:
		String siteName = "ITUserCreateSite${RUN_SUFFIX}"

		Map site = jsonws.createSite(siteName, 'open')
		long groupId = site.groupId as Long

		when: 'POST /ldf/user with groupIds'
		Map response = ldf.createUser([
			count   : 1,
			baseName: GROUP_BASE_NAME,
			groupIds: [groupId]
		])

		then:
		response.success == true

		when:
		Map createdUser = (response.items as List).first() as Map
		long userId = createdUser.userId as Long
		createdUserIds << userId

		and: 'the user appears in the site group via JSONWS get-user-sites-groups'
		List siteGroups = jsonwsGet(
			"group/get-user-sites-groups" +
			"/user-id/${userId}/start/-1/end/-1"
		) as List

		then:
		siteGroups?.any { (it.groupId as Long) == groupId }
	}

	def 'generates personal site layouts when toggle is on'() {
		when: 'POST /ldf/user with generatePersonalSiteLayouts=true'
		Map response = ldf.createUser([
			count                      : 1,
			baseName                   : LAYOUT_BASE_NAME,
			generatePersonalSiteLayouts: true
		])

		then:
		response.success == true

		when:
		Map createdUser = (response.items as List).first() as Map
		long userId = createdUser.userId as Long
		createdUserIds << userId

		and: 'take the personal site groupId from the creator response'
		// The User entity has no persistent groupId column, so JSONWS's
		// get-user-by-id does not echo it. UserCreator echoes the personal
		// site groupId on its response when generatePersonalSiteLayouts is
		// on.
		long userGroupId = createdUser.groupId as Long

		and: 'query public and private layouts via JSONWS'
		List publicLayouts = jsonwsGet(
			"layout/get-layouts" +
			"/group-id/${userGroupId}/private-layout/false"
		) as List

		List privateLayouts = jsonwsGet(
			"layout/get-layouts" +
			"/group-id/${userGroupId}/private-layout/true"
		) as List

		then: 'personal site has at least one public and one private layout'
		publicLayouts != null
		!publicLayouts.isEmpty()
		privateLayouts != null
		!privateLayouts.isEmpty()
	}

	def 'links public layout set prototype when specified'() {
		given:
		String protoName = "ITUserCreateProto${RUN_SUFFIX}"

		Map proto = jsonws.createLayoutSetPrototype(protoName)
		long prototypeId = proto.layoutSetPrototypeId as Long
		String prototypeUuid = proto.uuid as String

		when: 'POST /ldf/user with publicLayoutSetPrototypeId'
		Map response = ldf.createUser([
			count                      : 1,
			baseName                   : PROTO_BASE_NAME,
			generatePersonalSiteLayouts: true,
			publicLayoutSetPrototypeId : prototypeId
		])

		then:
		response.success == true

		when:
		Map createdUser = (response.items as List).first() as Map
		long userId = createdUser.userId as Long
		createdUserIds << userId

		then: 'layoutSetPrototypeUuid echoed by the creator matches the ' +
			'prototype uuid'
		// LayoutSetService does not expose getLayoutSet via JSONWS, so the
		// UserCreator now echoes back the linked prototype uuid on the
		// created-user JSON when generatePersonalSiteLayouts is on.
		(createdUser.publicLayoutSetPrototypeUuid as String) == prototypeUuid
	}

	def 'workflow path returns groupId and layoutSetPrototypeUuid when generatePersonalSiteLayouts is true'() {
		given:
		Map proto = jsonws.createLayoutSetPrototype("ITWFUserProto${RUN_SUFFIX}")
		long prototypeId = proto.layoutSetPrototypeId as Long
		String prototypeUuid = proto.uuid as String

		assert prototypeUuid != null && !prototypeUuid.isEmpty() : \
			"createLayoutSetPrototype did not return a uuid field"

		when: 'execute workflow with user.create and generatePersonalSiteLayouts=true'
		Map response = workflowHttpClient.execute([
			schemaVersion: '1.0',
			workflowId   : "wf-user-layout-${RUN_SUFFIX}",
			input        : [:],
			steps        : [
				[
					id            : 'createUser',
					idempotencyKey: "wf-user-layout-user-${RUN_SUFFIX}",
					operation     : 'user.create',
					onError       : [policy: 'FAIL_FAST'],
					params        : [
						[name: 'count', value: 1],
						[name: 'baseName', value: WF_LAYOUT_BASE_NAME],
						[name: 'generatePersonalSiteLayouts', value: true],
						[name: 'publicLayoutSetPrototypeId', value: prototypeId]
					]
				]
			]
		])

		then: 'workflow execution reports success'
		((response.execution as Map).status as String) == 'SUCCEEDED'

		when: 'extract user item from workflow step result'
		List<Map<String, Object>> steps = (response.execution as Map).steps as List<Map<String, Object>>
		Map userStep = steps.find { (it.stepId as String) == 'createUser' } as Map
		Map userResult = userStep.result as Map
		Map userItem = (userResult.items as List<Map<String, Object>>).first()
		long userId = userItem.userId as Long
		createdUserIds << userId

		then: 'groupId is present in workflow response'
		(userItem.groupId as Long) > 0L

		and: 'publicLayoutSetPrototypeUuid matches the pre-created prototype'
		(userItem.publicLayoutSetPrototypeUuid as String) == prototypeUuid
	}

}
