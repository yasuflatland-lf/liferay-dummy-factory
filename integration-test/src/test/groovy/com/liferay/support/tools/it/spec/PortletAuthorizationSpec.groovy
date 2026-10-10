package com.liferay.support.tools.it.spec

import com.liferay.support.tools.it.util.LdfResourceClient

import groovy.json.JsonOutput

import spock.lang.Shared

class PortletAuthorizationSpec extends BaseLiferaySpec {

	// Must differ from the password LdfResourceClient sets on the forced first
	// password change; DXP rejects a change to the current password.
	private static final String _DELEGATE_INITIAL_PASSWORD = 'Delegate123'

	@Shared
	LdfResourceClient adminClient

	@Shared
	LdfResourceClient delegateClient

	@Shared
	Long delegateUserId

	@Shared
	Long delegateRoleId

	@Shared
	String delegateEmail

	def setupSpec() {
		ensureBundleActive()
		adminClient = new LdfResourceClient(liferay.baseUrl)
		adminClient.login()

		String suffix = "${System.currentTimeMillis()}"
		String roleName = "ldf-delegate-${suffix}"
		Map role = jsonwsPost('role/add-role', [
			externalReferenceCode: '', className: 'com.liferay.portal.kernel.model.Role',
			classPK: 0, name: roleName, titleMap: JsonOutput.toJson([en_US: roleName]),
			descriptionMap: '{}', type: 1, subtype: ''
		]) as Map
		assert role.name == roleName : "wrong setup role: ${role}"
		delegateRoleId = role.roleId as Long
		assert delegateRoleId > 0

		// ResourcePermissionService.addResourcePermission supports company scope (1),
		// whose primKey is the companyId. Control Panel portlet access uses groupId 0.
		jsonwsPost('resourcepermission/add-resource-permission', [
			groupId: 0, companyId: companyId, name: PORTLET_ID, scope: 1,
			primKey: companyId.toString(), roleId: delegateRoleId,
			actionId: 'ACCESS_IN_CONTROL_PANEL'
		])

		// Reaching /group/control_panel at all also needs the portal-level
		// VIEW_CONTROL_PANEL ("90" is PortletKeys.PORTAL), as DXP grants it
		// alongside ACCESS_IN_CONTROL_PANEL in RoleLocalServiceImpl.
		jsonwsPost('resourcepermission/add-resource-permission', [
			groupId: 0, companyId: companyId, name: '90', scope: 1,
			primKey: companyId.toString(), roleId: delegateRoleId,
			actionId: 'VIEW_CONTROL_PANEL'
		])

		Map created = adminClient.createUser([
			count: 1, baseName: "ldfdelegate${suffix}", emailDomain: 'liferay.com',
			password: _DELEGATE_INITIAL_PASSWORD, fakerEnable: false
		])
		delegateUserId = created.items?.getAt(0)?.userId as Long
		assert created.error == null : "delegate creation failed: ${created}"
		assert created.success == true && delegateUserId > 0
		delegateEmail = created.items[0].emailAddress as String
		Map user = jsonwsGet(
			"user/get-user-by-email-address?companyId=${companyId}&emailAddress=${delegateEmail}") as Map
		assert (user.emailAddress as String).equalsIgnoreCase(delegateEmail) :
			"wrong setup user: ${user}"
		assert (user.userId as Long) == delegateUserId

		jsonwsPost('role/add-user-roles', [
			userId: delegateUserId, roleIds: JsonOutput.toJson([delegateRoleId])
		])

		delegateClient = new LdfResourceClient(
			liferay.baseUrl, delegateEmail, _DELEGATE_INITIAL_PASSWORD)
	}

	def cleanupSpec() {
		try {
			if (delegateUserId != null) {
				jsonwsPost('user/delete-user', [userId: delegateUserId])
			}
			if (delegateRoleId != null) {
				jsonwsPost('role/delete-role', [roleId: delegateRoleId])
			}
		}
		finally {
			delegateClient?.close()
			adminClient?.close()
		}
	}

	def 'a non-admin with portlet access cannot create an Administrator user'() {
		given:
		Map administrator = jsonwsGet(
			"role/get-role?companyId=${companyId}&name=Administrator") as Map
		assert administrator.name == 'Administrator' : "wrong role: ${administrator}"
		List roles = jsonwsGet("role/get-user-roles?userId=${delegateUserId}") as List
		assert roles.any { (it.roleId as Long) == delegateRoleId } : "access role missing: ${roles}"
		assert !roles.any { it.name == 'Administrator' } : "delegate is an admin: ${roles}"
		String baseName = "ldfdenied${System.currentTimeMillis()}"
		String email = "${baseName}1@liferay.com"

		expect: 'positive control: the delegate renders the portlet and receives its resource URLs'
		delegateClient.canReachPortlet()

		when:
		Map response = delegateClient.createUser([
			count: 1, baseName: baseName, emailDomain: 'liferay.com',
			password: NEW_ADMIN_PASSWORD, roleIds: [administrator.roleId as Long],
			fakerEnable: false
		])

		then:
		assert response.containsKey('success') && response.containsKey('error') :
			"unexpected denial shape: ${response}"
		assert response.success == false : "delegate created an admin: ${response}"
		assert response.error == 'Creating data requires a company administrator.' :
			"rejection did not come from the shared authorization check: ${response}"

		when:
		Map control = _lookupUser(delegateEmail)
		Map lookup = _lookupUser(email)

		then: 'the same lookup finds an existing user, so the 404 means absence'
		assert control.status == 200 && control.body.contains(delegateEmail) :
			"lookup of the existing delegate failed: ${control}"
		assert lookup.status == 404 :
			"rejected request created a user or lookup failed unexpectedly: ${lookup}"
	}

	private Map _lookupUser(String email) {
		HttpURLConnection connection = new URL(jsonwsUrl(
			"user/get-user-by-email-address?companyId=${companyId}&emailAddress=" +
				URLEncoder.encode(email, 'UTF-8'))).openConnection() as HttpURLConnection
		connection.setRequestProperty('Authorization', basicAuthHeader())
		connection.setRequestProperty('Accept', 'application/json')
		connection.setRequestProperty('Accept-Encoding', 'identity')
		connection.connectTimeout = 30_000
		connection.readTimeout = 30_000

		try {
			int status = connection.responseCode
			String body = (status >= 400 ? connection.errorStream : connection.inputStream)?.getText('UTF-8') ?: ''
			return [status: status, body: body]
		}
		finally {
			connection.disconnect()
		}
	}

}
