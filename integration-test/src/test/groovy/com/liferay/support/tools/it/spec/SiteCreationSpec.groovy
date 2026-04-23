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
class SiteCreationSpec extends BaseLiferaySpec {

	private static final int TYPE_SITE_OPEN = 1

	private static final int TYPE_SITE_PRIVATE = 3

	private static final String RUN_SUFFIX = String.valueOf(System.currentTimeMillis())

	private static final String BASE_OPEN_SITE = "ITOpenSite${RUN_SUFFIX}"
	private static final String BASE_PRIVATE_SITE = "ITPrivateSite${RUN_SUFFIX}"
	private static final String BASE_PROTOTYPE_SITE = "ITProtoSite${RUN_SUFFIX}"
	private static final String BASE_CHILD_SITE = "ITChildSite${RUN_SUFFIX}"
	private static final String WF_CHILD_SITE_BASE = "wfchildsite${RUN_SUFFIX}"
	private static final String WF_PROTO_SITE_BASE = "wfprotosite${RUN_SUFFIX}"

	private static final Logger log = LoggerFactory.getLogger(SiteCreationSpec)

	@Shared
	LdfResourceClient ldf

	@Shared
	JsonwsSetupHelper jsonws

	@Shared
	PlaywrightLifecycle pw

	@Shared
	WorkflowHttpClient workflowHttpClient

	@Shared
	List<Long> createdSiteIds = []

	def setupSpec() {
		ensureBundleActive()

		ldf = new LdfResourceClient("http://localhost:${liferay.httpPort}")

		// Log in the admin session so JsonwsSetupHelper (which uses Basic Auth against
		// JSONWS) runs under an already-primed cookie store. D2 removed the
		// password-change detour; this is now a one-shot login.
		ldf.login()

		jsonws = new JsonwsSetupHelper(
			"http://localhost:${liferay.httpPort}")

		pw = new PlaywrightLifecycle()
		loginAsAdmin(pw)
		workflowHttpClient = new WorkflowHttpClient(
			"http://localhost:${liferay.httpPort}", pw.page)
	}

	def cleanupSpec() {
		createdSiteIds.each { Long id ->
			try {
				jsonwsPost(
					'group/delete-group',
					['groupId': id])
			}
			catch (Exception e) {
				log.warn('Failed to clean up site {}: {}', id, e.message)
			}
		}

		jsonws?.cleanupAll()
		ldf?.close()
		pw?.close()
	}

	def 'creates sites with basic fields and open membership'() {
		given:
		Map fields = [
			count: 3,
			baseName: BASE_OPEN_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true
		]

		when: 'POST /ldf/site with open membership'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 3
		(response.items as List)?.size() == 3

		and: 'response shape contract is complete'
		response.containsKey('success')
		response.containsKey('count')
		response.containsKey('requested')
		response.containsKey('skipped')
		response.containsKey('items')
		(response.requested as Integer) == 3
		(response.skipped as Integer) == 0

		when: 'collect created site ids for JSONWS verification'
		List<Long> groupIds = (response.items as List).collect {
			(it as Map).groupId as Long
		}
		createdSiteIds.addAll(groupIds)

		then: 'every created group exists and is an open site'
		groupIds.every { Long groupId ->
			Map group = jsonwsGet(
				"group/get-group/group-id/${groupId}") as Map
			(group?.type as Integer) == TYPE_SITE_OPEN
		}
	}

	def 'creates sites with private membership type'() {
		given:
		Map fields = [
			count: 2,
			baseName: BASE_PRIVATE_SITE,
			membershipType: 'private',
			active: true,
			manualMembership: true
		]

		when: 'POST /ldf/site with private membership'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 2

		when: 'collect created site ids for JSONWS verification'
		List<Long> groupIds = (response.items as List).collect {
			(it as Map).groupId as Long
		}
		createdSiteIds.addAll(groupIds)

		then: 'every created group is a private site'
		groupIds.every { Long groupId ->
			Map group = jsonwsGet(
				"group/get-group/group-id/${groupId}") as Map
			(group?.type as Integer) == TYPE_SITE_PRIVATE
		}
	}

	def 'links public and private layout set prototypes to created sites'() {
		given: 'pre-create two layout set prototypes'
		Map publicProto = jsonws.createLayoutSetPrototype(
			"ITPubProto${RUN_SUFFIX}")
		Map privateProto = jsonws.createLayoutSetPrototype(
			"ITPrivProto${RUN_SUFFIX}")

		Long publicProtoId = publicProto.layoutSetPrototypeId as Long
		Long privateProtoId = privateProto.layoutSetPrototypeId as Long

		and: 'fetch prototype uuids via JSONWS'
		Map publicProtoDetail = jsonwsGet(
			"layoutsetprototype/get-layout-set-prototype" +
			"/layout-set-prototype-id/${publicProtoId}") as Map
		Map privateProtoDetail = jsonwsGet(
			"layoutsetprototype/get-layout-set-prototype" +
			"/layout-set-prototype-id/${privateProtoId}") as Map

		String expectedPublicUuid = publicProtoDetail.uuid as String
		String expectedPrivateUuid = privateProtoDetail.uuid as String

		Map fields = [
			count: 1,
			baseName: BASE_PROTOTYPE_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true,
			publicLayoutSetPrototypeId: publicProtoId,
			privateLayoutSetPrototypeId: privateProtoId
		]

		when: 'POST /ldf/site with both prototype ids'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 1

		when: 'collect created site id and prototype uuids from the response'
		Map created = (response.items as List).first() as Map
		Long createdGroupId = created.groupId as Long
		createdSiteIds << createdGroupId

		then: 'each layout set is linked to the pre-created prototype uuid'
		// LayoutSetService does not expose getLayoutSet via JSONWS, so the
		// SiteCreator now echoes back the linked prototype uuids on its
		// response payload. See SiteCreator#create.
		(created.publicLayoutSetPrototypeUuid as String) == expectedPublicUuid
		(created.privateLayoutSetPrototypeUuid as String) == expectedPrivateUuid
	}

	def 'inherits content from parent site when inheritContent=true'() {
		given: 'pre-create a parent site'
		Map parentSite = jsonws.createSite(
			"ITParentSite${RUN_SUFFIX}", 'open')
		Long parentGroupId = parentSite.groupId as Long

		Map fields = [
			count: 1,
			baseName: BASE_CHILD_SITE,
			membershipType: 'open',
			active: true,
			manualMembership: true,
			parentGroupId: parentGroupId,
			inheritContent: true
		]

		when: 'POST /ldf/site with parentGroupId and inheritContent=true'
		Map response = ldf.createSite(fields)

		then: 'creator reports success'
		response.success == true
		(response.count as Integer) == 1

		when: 'collect created child site id and parent metadata from response'
		// SiteCreator echoes back inheritContent and parentGroupId so we do
		// not need to round-trip through group/get-group, which
		// would not expose inheritContent on the default Group JSON view.
		Map created = (response.items as List).first() as Map
		Long childGroupId = created.groupId as Long
		createdSiteIds << childGroupId

		then: 'child group records parent relationship and inheritContent flag'
		(created.parentGroupId as Long) == parentGroupId
		(created.inheritContent as Boolean) == true
	}

	def 'workflow path returns inheritContent and parentGroupId'() {
		given: 'pre-create a parent site via JSONWS'
		Map parentSite = jsonws.createSite("ITWFParentSite${RUN_SUFFIX}", 'open')
		long parentGroupId = parentSite.groupId as Long

		when: 'execute workflow with site.create, parentGroupId and inheritContent=true'
		Map response = workflowHttpClient.execute([
			schemaVersion: '1.0',
			workflowId   : "wf-site-inherit-${RUN_SUFFIX}",
			input        : [:],
			steps        : [
				[
					id            : 'createSite',
					idempotencyKey: "wf-site-inherit-site-${RUN_SUFFIX}",
					operation     : 'site.create',
					onError       : [policy: 'FAIL_FAST'],
					params        : [
						[name: 'count', value: 1],
						[name: 'baseName', value: WF_CHILD_SITE_BASE],
						[name: 'parentGroupId', value: parentGroupId],
						[name: 'inheritContent', value: true]
					]
				]
			]
		])

		then: 'workflow execution reports success'
		((response.execution as Map).status as String) == 'SUCCEEDED'

		when: 'extract site item from workflow step result'
		List<Map<String, Object>> steps = (response.execution as Map).steps as List<Map<String, Object>>
		Map siteStep = steps.find { (it.stepId as String) == 'createSite' } as Map
		Map siteResult = siteStep.result as Map
		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		long childGroupId = siteItem.groupId as Long
		createdSiteIds << childGroupId

		then: 'parentGroupId and inheritContent are present in workflow response'
		(siteItem.parentGroupId as Long) == parentGroupId
		(siteItem.inheritContent as Boolean) == true
	}

	def 'workflow path returns layoutSetPrototypeUuids'() {
		given: 'pre-create two layout set prototypes'
		Map pubProto = jsonws.createLayoutSetPrototype("ITWFSitePubProto${RUN_SUFFIX}")
		Map privProto = jsonws.createLayoutSetPrototype("ITWFSitePrivProto${RUN_SUFFIX}")
		long pubProtoId = pubProto.layoutSetPrototypeId as Long
		long privProtoId = privProto.layoutSetPrototypeId as Long
		String expectedPubUuid = pubProto.uuid as String
		String expectedPrivUuid = privProto.uuid as String

		assert expectedPubUuid != null && !expectedPubUuid.isEmpty() : \
			"createLayoutSetPrototype did not return a uuid field"
		assert expectedPrivUuid != null && !expectedPrivUuid.isEmpty() : \
			"createLayoutSetPrototype did not return a uuid field"

		when: 'execute workflow with site.create and both prototype ids'
		Map response = workflowHttpClient.execute([
			schemaVersion: '1.0',
			workflowId   : "wf-site-proto-${RUN_SUFFIX}",
			input        : [:],
			steps        : [
				[
					id            : 'createSite',
					idempotencyKey: "wf-site-proto-site-${RUN_SUFFIX}",
					operation     : 'site.create',
					onError       : [policy: 'FAIL_FAST'],
					params        : [
						[name: 'count', value: 1],
						[name: 'baseName', value: WF_PROTO_SITE_BASE],
						[name: 'publicLayoutSetPrototypeId', value: pubProtoId],
						[name: 'privateLayoutSetPrototypeId', value: privProtoId]
					]
				]
			]
		])

		then: 'workflow execution reports success'
		((response.execution as Map).status as String) == 'SUCCEEDED'

		when: 'extract site item from workflow step result'
		List<Map<String, Object>> steps = (response.execution as Map).steps as List<Map<String, Object>>
		Map siteStep = steps.find { (it.stepId as String) == 'createSite' } as Map
		Map siteResult = siteStep.result as Map
		Map siteItem = (siteResult.items as List<Map<String, Object>>).first()
		long groupId = siteItem.groupId as Long
		createdSiteIds << groupId

		then: 'layoutSetPrototypeUuids match pre-created prototypes'
		(siteItem.publicLayoutSetPrototypeUuid as String) == expectedPubUuid
		(siteItem.privateLayoutSetPrototypeUuid as String) == expectedPrivUuid
	}

}
