package com.liferay.support.tools.workflow;

import java.util.List;
import java.util.Map;

final class WorkflowFunctionDescriptors {

	static Map<String, WorkflowFunctionDescriptor> descriptors() {
		return Map.ofEntries(
			_coreDescriptors(),
			_coreOrganizationDescriptors(),
			_coreRoleDescriptors(),
			_coreSiteDescriptors(),
			_coreUserDescriptors(),
			_contentDescriptors(),
			_contentDocumentDescriptors(),
			_contentLayoutDescriptors(),
			_contentWebContentDescriptors(),
			_messageBoardsDescriptors(),
			_messageBoardsReplyDescriptors(),
			_messageBoardsThreadDescriptors(),
			_taxonomyDescriptors(),
			_taxonomyVocabularyDescriptors());
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_coreDescriptors() {

		return Map.entry(
			"company.create",
			new WorkflowFunctionDescriptor(
				"company.create", "Create virtual instances (companies).",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of companies to create.", null),
					parameter(
						"webId", "string", true, "Company web id prefix.",
						null),
					parameter(
						"virtualHostname", "string", true, "Virtual host name.",
						null),
					parameter("mx", "string", true, "Mail domain.", null),
					parameter(
						"maxUsers", "integer", false,
						"Maximum users for the company.", 0),
					parameter(
						"active", "boolean", false,
						"Whether the company is active.", true)),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_coreOrganizationDescriptors() {

		return Map.entry(
			"organization.create",
			new WorkflowFunctionDescriptor(
				"organization.create", "Create organizations.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of organizations to create.", null),
					parameter(
						"baseName", "string", true,
						"Base organization name.", null),
					parameter(
						"parentOrganizationId", "long", false,
						"Parent organization id.", 0),
					parameter(
						"site", "boolean", false,
						"Whether to create an organization site.", false)),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_coreRoleDescriptors() {

		return Map.entry(
			"role.create",
			new WorkflowFunctionDescriptor(
				"role.create", "Create roles.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of roles to create.", null),
					parameter(
						"baseName", "string", true, "Base role name.", null),
					parameter(
						"roleType", "string", false, "Role type.", "regular"),
					parameter(
						"description", "string", false, "Role description.",
						"")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_coreSiteDescriptors() {

		return Map.entry(
			"site.create",
			new WorkflowFunctionDescriptor(
				"site.create", "Create sites.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of sites to create.", null),
					parameter(
						"baseName", "string", true, "Base site name.", null),
					parameter(
						"membershipType", "string", false,
						"Membership type: open, restricted, private.", "open"),
					parameter(
						"parentGroupId", "long", false,
						"Parent site group id.", 0),
					parameter(
						"siteTemplateId", "long", false, "Site template id.",
						0),
					parameter(
						"publicLayoutSetPrototypeId", "long", false,
						"Public layout set prototype id.", 0),
					parameter(
						"privateLayoutSetPrototypeId", "long", false,
						"Private layout set prototype id.", 0),
					parameter(
						"manualMembership", "boolean", false,
						"Whether manual membership is required.", true),
					parameter(
						"inheritContent", "boolean", false,
						"Whether content is inherited.", false),
					parameter(
						"active", "boolean", false, "Whether the site is active.",
						true),
					parameter(
						"description", "string", false, "Site description.",
						"")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_coreUserDescriptors() {

		return Map.entry(
			"user.create",
			new WorkflowFunctionDescriptor(
				"user.create", "Create users.",
				List.of(
					parameter(
						"count", "integer", true, "Number of users to create.",
						null),
					parameter(
						"baseName", "string", true, "Base user name.", null),
					parameter(
						"emailDomain", "string", false, "Email domain.",
						"liferay.com"),
					parameter(
						"password", "string", false, "Default password.",
						"test"),
					parameter(
						"male", "boolean", false,
						"Whether generated users are male.", true),
					parameter("jobTitle", "string", false, "Job title.", ""),
					parameter(
						"fakerEnable", "boolean", false,
						"Use faker profile generation.", false),
					parameter("locale", "string", false, "Locale id.", "en_US"),
					parameter(
						"organizationIds", "long[]", false, "Organization ids.",
						List.of()),
					parameter("roleIds", "long[]", false, "Role ids.", List.of()),
					parameter(
						"userGroupIds", "long[]", false, "User group ids.",
						List.of()),
					parameter(
						"siteRoleIds", "long[]", false, "Site role ids.",
						List.of()),
					parameter(
						"orgRoleIds", "long[]", false,
						"Organization role ids.", List.of()),
					parameter(
						"groupIds", "long[]", false, "Site group ids.",
						List.of()),
					parameter(
						"generatePersonalSiteLayouts", "boolean", false,
						"Generate personal site layouts.", false),
					parameter(
						"publicLayoutSetPrototypeId", "long", false,
						"Public layout set prototype id.", 0),
					parameter(
						"privateLayoutSetPrototypeId", "long", false,
						"Private layout set prototype id.", 0)),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_contentDescriptors() {

		return Map.entry(
			"blogs.create",
			new WorkflowFunctionDescriptor(
				"blogs.create", "Create blog entries in a target site.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of blogs to create.", null),
					parameter(
						"baseName", "string", true,
						"Base title used for generated blogs.", null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user"),
					parameter("content", "string", false, "Body content.", ""),
					parameter("subtitle", "string", false, "Blog subtitle.", ""),
					parameter(
						"description", "string", false,
						"Blog description.", ""),
					parameter(
						"allowPingbacks", "boolean", false,
						"Whether pingbacks are enabled.", false),
					parameter(
						"allowTrackbacks", "boolean", false,
						"Whether trackbacks are enabled.", false),
					parameter(
						"trackbackURLs", "string[]", false,
						"Trackback target URLs.", List.of())),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_contentDocumentDescriptors() {

		return Map.entry(
			"document.create",
			new WorkflowFunctionDescriptor(
				"document.create", "Create documents in a target folder.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of documents to create.", null),
					parameter(
						"baseName", "string", true, "Base document title.",
						null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter("folderId", "long", false, "Target folder id.", 0),
					parameter(
						"description", "string", false,
						"Document description.", ""),
					parameter(
						"uploadedFiles", "string[]", false,
						"Template file paths.", List.of())),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_contentLayoutDescriptors() {

		return Map.entry(
			"layout.create",
			new WorkflowFunctionDescriptor(
				"layout.create", "Create pages in a target site.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of pages to create.", null),
					parameter(
						"baseName", "string", true, "Base page name.", null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"type", "string", false, "Layout type.", "portlet"),
					parameter(
						"privateLayout", "boolean", false,
						"Whether pages are private.", false),
					parameter(
						"hidden", "boolean", false, "Whether pages are hidden.",
						false)),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_contentWebContentDescriptors() {

		return Map.entry(
			"webContent.create",
			new WorkflowFunctionDescriptor(
				"webContent.create", "Create web content articles.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of web contents to create.", null),
					parameter(
						"baseName", "string", true, "Base article title.",
						null),
					parameter(
						"groupIds", "long[]", true, "Target site group ids.",
						null),
					parameter("folderId", "long", false, "Target folder id.", 0),
					parameter(
						"locales", "string[]", false,
						"Explicit locales. If omitted, site default locale is used.",
						List.of()),
					parameter(
						"neverExpire", "boolean", false,
						"Whether the content never expires.", true),
					parameter(
						"neverReview", "boolean", false,
						"Whether the content never requires review.", true),
					parameter(
						"createContentsType", "integer", false,
						"Content creation mode.", 0),
					parameter(
						"baseArticle", "string", false, "Base article content.",
						""),
					parameter(
						"titleWords", "integer", false,
						"Generated title word count.", 5),
					parameter(
						"totalParagraphs", "integer", false,
						"Generated paragraph count.", 3),
					parameter(
						"randomAmount", "integer", false,
						"Random content amount.", 3),
					parameter(
						"linkLists", "string", false, "Link list configuration.",
						""),
					parameter(
						"ddmStructureId", "long", false, "DDM structure id.", 0),
					parameter(
						"ddmTemplateId", "long", false, "DDM template id.", 0)),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_messageBoardsDescriptors() {

		return Map.entry(
			"mbCategory.create",
			new WorkflowFunctionDescriptor(
				"mbCategory.create", "Create message boards categories.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of categories to create.", null),
					parameter(
						"baseName", "string", true,
						"Base category name.", null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"description", "string", true,
						"Category description.", null),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_messageBoardsReplyDescriptors() {

		return Map.entry(
			"mbReply.create",
			new WorkflowFunctionDescriptor(
				"mbReply.create", "Create replies in a message board thread.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of replies to create.", null),
					parameter(
						"threadId", "long", true, "Target thread id.", null),
					parameter("body", "string", true, "Reply body.", null),
					parameter("format", "string", false, "Reply format.", "html"),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_messageBoardsThreadDescriptors() {

		return Map.entry(
			"mbThread.create",
			new WorkflowFunctionDescriptor(
				"mbThread.create", "Create message board threads.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of threads to create.", null),
					parameter(
						"baseName", "string", true, "Base thread subject.",
						null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"categoryId", "long", false,
						"Target category id. Use 0 for root.", 0),
					parameter("body", "string", true, "Thread body.", null),
					parameter("format", "string", false, "Thread body format.", "html"),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_taxonomyDescriptors() {

		return Map.entry(
			"category.create",
			new WorkflowFunctionDescriptor(
				"category.create", "Create asset categories in a vocabulary.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of categories to create.", null),
					parameter(
						"baseName", "string", true,
						"Base category name.", null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"vocabularyId", "long", true,
						"Target vocabulary id.", null),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user")),
				"WorkflowStepResult"));
	}

	private static Map.Entry<String, WorkflowFunctionDescriptor>
		_taxonomyVocabularyDescriptors() {

		return Map.entry(
			"vocabulary.create",
			new WorkflowFunctionDescriptor(
				"vocabulary.create", "Create asset vocabularies.",
				List.of(
					parameter(
						"count", "integer", true,
						"Number of vocabularies to create.", null),
					parameter(
						"baseName", "string", true, "Base vocabulary name.",
						null),
					parameter(
						"groupId", "long", true, "Target site group id.",
						null),
					parameter(
						"userId", "long", false,
						"Override execution user id.", "current user")),
				"WorkflowStepResult"));
	}

	private static WorkflowFunctionParameter parameter(
		String name, String type, boolean required, String description,
		Object defaultValue) {

		return new WorkflowFunctionParameter(
			name, type, required, description, defaultValue);
	}

	private WorkflowFunctionDescriptors() {
	}

}
