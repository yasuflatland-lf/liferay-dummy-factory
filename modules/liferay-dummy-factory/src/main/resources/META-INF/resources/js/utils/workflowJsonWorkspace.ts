import {
	ApiResponse,
	WorkflowExecuteResponse,
	WorkflowPlanResponse,
	WorkflowRequestPayload,
} from '../types';
import {postJsonResource} from './api';
import {translate} from './i18n';
import {
	validateWorkflowJsonText as _schemaValidate,
} from './workflowJsonSchema';

export interface WorkflowJsonSample {
	descriptionKey: string;
	id: string;
	json: string;
	operations: string[];
	titleKey: string;
}

export type WorkflowJsonResultTone = 'danger' | 'success' | 'warning';

export interface WorkflowJsonWorkspaceResult {
	action: 'ajv' | 'execute' | 'load' | 'plan';
	actionLabel: string;
	body: string;
	title: string;
	tone: WorkflowJsonResultTone;
}

const _sampleDefinitions: Array<
	Omit<WorkflowJsonSample, 'json'> & {request: WorkflowRequestPayload}
> = [
	{
		descriptionKey: 'workflow-json-sample-site-description',
		id: 'site-and-page',
		operations: ['site.create', 'layout.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-site-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-site'},
					],
				},
				{
					id: 'createPage',
					idempotencyKey: 'sample-page-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'layout.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'welcome'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{name: 'type', value: 'portlet'},
					],
				},
			],
			workflowId: 'sample-site-and-page',
		},
		titleKey: 'workflow-json-sample-site-title',
	},
	{
		descriptionKey: 'workflow-json-sample-company-description',
		id: 'company-user-organization',
		operations: ['company.create', 'user.create', 'organization.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createCompany',
					idempotencyKey: 'sample-company-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'company.create',
					params: [
						{name: 'count', value: 1},
						{name: 'webId', value: 'sample-workflow-company'},
						{
							name: 'virtualHostname',
							value: 'sample-workflow-company.local',
						},
						{name: 'mx', value: 'sample-workflow-company.local'},
					],
				},
				{
					id: 'createUser',
					idempotencyKey: 'sample-user-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'user.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-workflow-user'},
						{name: 'emailDomain', value: 'example.com'},
					],
				},
				{
					id: 'createOrganization',
					idempotencyKey: 'sample-org-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'organization.create',
					params: [
						{name: 'count', value: 1},
						{
							name: 'baseName',
							value: 'sample-workflow-organization',
						},
					],
				},
			],
			workflowId: 'sample-company-user-organization',
		},
		titleKey: 'workflow-json-sample-company-title',
	},
	{
		descriptionKey: 'workflow-json-sample-taxonomy-description',
		id: 'vocabulary-and-category',
		operations: ['site.create', 'vocabulary.create', 'category.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-vocabulary-site-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-vocabulary-site'},
					],
				},
				{
					id: 'createVocabulary',
					idempotencyKey: 'sample-vocabulary-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'vocabulary.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-vocabulary'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
					],
				},
				{
					id: 'createCategory',
					idempotencyKey: 'sample-category-1',
					onError: {
						policy: 'FAIL_FAST',
					},
					operation: 'category.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-category'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{
							name: 'vocabularyId',
							from: 'steps.createVocabulary.items[0].vocabularyId',
						},
					],
				},
			],
			workflowId: 'sample-vocabulary-and-category',
		},
		titleKey: 'workflow-json-sample-taxonomy-title',
	},
	{
		descriptionKey: 'workflow-json-sample-role-description',
		id: 'role',
		operations: ['role.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createRole',
					idempotencyKey: 'sample-role-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'role.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-workflow-role'},
						{name: 'roleType', value: 'REGULAR'},
						{
							name: 'description',
							value: 'Sample role created by workflow template.',
						},
					],
				},
			],
			workflowId: 'sample-role',
		},
		titleKey: 'workflow-json-sample-role-title',
	},
	{
		descriptionKey: 'workflow-json-sample-documents-description',
		id: 'documents',
		operations: ['site.create', 'document.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-doc-site-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-doc-site'},
					],
				},
				{
					id: 'createDocument',
					idempotencyKey: 'sample-document-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'document.create',
					params: [
						{name: 'count', value: 2},
						{name: 'baseName', value: 'sample-workflow-document'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{
							name: 'description',
							value: 'Sample document generated by workflow template.',
						},
					],
				},
			],
			workflowId: 'sample-documents',
		},
		titleKey: 'workflow-json-sample-documents-title',
	},
	{
		descriptionKey: 'workflow-json-sample-blogs-and-web-content-description',
		id: 'blogs-and-web-content',
		operations: ['site.create', 'blogs.create', 'webContent.create'],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-blog-wc-site-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-blog-wc-site'},
					],
				},
				{
					id: 'createBlogs',
					idempotencyKey: 'sample-blog-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'blogs.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-workflow-blog'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{name: 'subtitle', value: 'Sample blog subtitle'},
						{name: 'content', value: 'Sample blog entry content.'},
					],
				},
				{
					id: 'createWebContent',
					idempotencyKey: 'sample-web-content-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'webContent.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-workflow-article'},
						{
							name: 'groupIds',
							from: 'steps.createSite.items[0].groupId',
						},
					],
				},
			],
			workflowId: 'sample-blogs-and-web-content',
		},
		titleKey: 'workflow-json-sample-blogs-and-web-content-title',
	},
	{
		descriptionKey: 'workflow-json-sample-message-boards-description',
		id: 'message-boards',
		operations: [
			'site.create',
			'mbCategory.create',
			'mbThread.create',
			'mbReply.create',
		],
		request: {
			input: {},
			schemaVersion: '1.0',
			steps: [
				{
					id: 'createSite',
					idempotencyKey: 'sample-mb-site-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'site.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-mb-site'},
					],
				},
				{
					id: 'createMBCategory',
					idempotencyKey: 'sample-mb-category-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'mbCategory.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-mb-category'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{
							name: 'description',
							value: 'Sample message board category.',
						},
					],
				},
				{
					id: 'createMBThread',
					idempotencyKey: 'sample-mb-thread-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'mbThread.create',
					params: [
						{name: 'count', value: 1},
						{name: 'baseName', value: 'sample-mb-thread'},
						{
							name: 'groupId',
							from: 'steps.createSite.items[0].groupId',
						},
						{
							name: 'categoryId',
							from: 'steps.createMBCategory.items[0].categoryId',
						},
						{
							name: 'body',
							value: 'Sample message board thread body.',
						},
					],
				},
				{
					id: 'createMBReply',
					idempotencyKey: 'sample-mb-reply-1',
					onError: {policy: 'FAIL_FAST'},
					operation: 'mbReply.create',
					params: [
						{name: 'count', value: 1},
						{
							name: 'threadId',
							from: 'steps.createMBThread.items[0].threadId',
						},
						{
							name: 'body',
							value: 'Sample message board reply body.',
						},
					],
				},
			],
			workflowId: 'sample-message-boards',
		},
		titleKey: 'workflow-json-sample-message-boards-title',
	},
];

async function _submitWorkflowJson<T>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	const result = await _schemaValidate(jsonText);

	if (result.kind === 'empty') {
		return {
			error: translate('workflow-json-empty-error'),
			success: false,
		};
	}

	if (result.kind === 'invalid') {
		const first = result.errors[0];

		return {
			error: first
				? `${first.path} ${first.message}`.trim()
				: translate('workflow-json-invalid-error'),
			success: false,
		};
	}

	return postJsonResource<T>(resourceURL, result.value);
}

function getWorkflowJsonSample(
	sampleId: string
): WorkflowJsonSample | null {
	return (
		getWorkflowJsonSamples().find((sample) => sample.id === sampleId) ??
		null
	);
}

export function getWorkflowJsonSamples(): WorkflowJsonSample[] {
	return _sampleDefinitions.map(({request, ...sample}) => ({
		...sample,
		json: JSON.stringify(request, null, 2),
	}));
}

export function loadWorkflowJsonSample(sampleId: string): string | null {
	return getWorkflowJsonSample(sampleId)?.json ?? null;
}

export async function planWorkflowJson<T = WorkflowPlanResponse>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	return _submitWorkflowJson<T>(resourceURL, jsonText);
}

export async function executeWorkflowJson<T = WorkflowExecuteResponse>(
	resourceURL: string,
	jsonText: string
): Promise<ApiResponse<T>> {
	return _submitWorkflowJson<T>(resourceURL, jsonText);
}
