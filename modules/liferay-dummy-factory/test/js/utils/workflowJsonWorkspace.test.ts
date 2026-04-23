import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mockPostJsonResource = vi.fn();

vi.mock('../../../src/main/resources/META-INF/resources/js/utils/api', () => ({
	postJsonResource: (...args: unknown[]) => mockPostJsonResource(...args),
}));

import {
	executeWorkflowJson,
	getWorkflowJsonSamples,
	loadWorkflowJsonSample,
	planWorkflowJson,
} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonWorkspace';
import {_resetSchemaForTest} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonSchema';

const VALID_SCHEMA = {
	$schema: 'https://json-schema.org/draft/2020-12/schema',
	additionalProperties: false,
	properties: {
		input: {type: 'object'},
		schemaVersion: {const: '1.0', type: 'string'},
		steps: {
			items: {
				additionalProperties: false,
				properties: {
					id: {type: 'string'},
					idempotencyKey: {type: 'string'},
					onError: {
						additionalProperties: false,
						properties: {
							policy: {enum: ['FAIL_FAST'], type: 'string'},
						},
						type: 'object',
					},
					operation: {type: 'string'},
					params: {
						items: {
							oneOf: [
								{
									additionalProperties: false,
									properties: {
										name: {type: 'string'},
										value: {},
									},
									required: ['name', 'value'],
									type: 'object',
								},
								{
									additionalProperties: false,
									properties: {
										from: {type: 'string'},
										name: {type: 'string'},
									},
									required: ['name', 'from'],
									type: 'object',
								},
							],
						},
						type: 'array',
					},
				},
				required: ['id', 'operation', 'idempotencyKey'],
				type: 'object',
			},
			minItems: 1,
			type: 'array',
		},
		workflowId: {type: 'string'},
	},
	required: ['schemaVersion', 'steps'],
	type: 'object',
};

describe('workflowJsonWorkspace samples', () => {
	beforeEach(() => {
		_resetSchemaForTest();
		mockPostJsonResource.mockReset();
	});

	it('loads a selected sample into the editor as formatted json', () => {
		const json = loadWorkflowJsonSample('site-and-page');

		expect(json).not.toBeNull();

		const parsed = JSON.parse(json!);

		expect(parsed.workflowId).toBe('sample-site-and-page');
		expect(parsed.steps).toHaveLength(2);
		expect(json).toContain('\n  "steps": [\n');
	});

	it('lists curated samples with action-oriented summaries', () => {
		const samples = getWorkflowJsonSamples();

		expect(samples.map((sample) => sample.id)).toEqual([
			'site-and-page',
			'company-user-organization',
			'vocabulary-and-category',
			'role',
			'documents',
			'blogs-and-web-content',
			'message-boards',
		]);
		expect(Liferay.Language.get(samples[2].descriptionKey)).toBe(
			'Create a vocabulary, then a category.'
		);

		const messageBoardsSample = samples.find((s) => s.id === 'message-boards')!;
		const messageBoardsTitle = Liferay.Language.get(messageBoardsSample.titleKey);

		expect(messageBoardsTitle).not.toBe(messageBoardsSample.titleKey);
		expect(messageBoardsTitle).toBe('Message boards');
	});

	it('returns null for an unknown sample id', () => {
		expect(loadWorkflowJsonSample('missing-sample-id')).toBeNull();
	});
});

describe('workflowJsonWorkspace validation and actions', () => {
	beforeEach(() => {
		_resetSchemaForTest();
		mockPostJsonResource.mockReset();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockResolvedValue({
				json: () => Promise.resolve({schema: VALID_SCHEMA}),
				ok: true,
			})
		);
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it('surfaces json parse errors before any server request', async () => {
		const result = await planWorkflowJson(
			'/o/ldf-workflow/plan',
			'{"oops"'
		);

		expect(result.success).toBe(false);
		expect(result.error).toBeTruthy();
		expect(mockPostJsonResource).not.toHaveBeenCalled();
	});

	it('short-circuits empty editor without hitting the endpoint', async () => {
		const result = await planWorkflowJson('/o/ldf-workflow/plan', '');

		expect(result.success).toBe(false);
		expect(result.error).toBeTruthy();
		expect(mockPostJsonResource).not.toHaveBeenCalled();
	});

	it('short-circuits schema-invalid payload without hitting the endpoint', async () => {
		const result = await planWorkflowJson(
			'/o/ldf-workflow/plan',
			JSON.stringify({schemaVersion: '1.0', steps: []})
		);

		expect(result.success).toBe(false);
		expect(result.error).toBeTruthy();
		expect(mockPostJsonResource).not.toHaveBeenCalled();
	});

	it('wires plan to the plan endpoint with the selected sample payload', async () => {
		mockPostJsonResource.mockResolvedValueOnce({
			data: {errors: [], plan: []},
			success: true,
		});

		const json = loadWorkflowJsonSample('company-user-organization')!;

		await planWorkflowJson('/o/ldf-workflow/plan', json);

		expect(mockPostJsonResource).toHaveBeenCalledWith(
			'/o/ldf-workflow/plan',
			expect.objectContaining({
				workflowId: 'sample-company-user-organization',
			})
		);
	});

	it('wires execute to the execute endpoint with the current editor payload', async () => {
		mockPostJsonResource.mockResolvedValueOnce({
			data: {execution: {status: 'SUCCEEDED'}},
			success: true,
		});

		const json = loadWorkflowJsonSample('vocabulary-and-category')!;

		await executeWorkflowJson('/o/ldf-workflow/execute', json);

		expect(mockPostJsonResource).toHaveBeenCalledWith(
			'/o/ldf-workflow/execute',
			expect.objectContaining({
				workflowId: 'sample-vocabulary-and-category',
			})
		);
	});

	it('returns error without calling server when schema is unavailable', async () => {
		_resetSchemaForTest();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockRejectedValue(new Error('Network error'))
		);

		const result = await planWorkflowJson(
			'/o/ldf-workflow/plan',
			JSON.stringify({
				schemaVersion: '1.0',
				steps: [
					{id: 'a', idempotencyKey: 'x', operation: 'noop'},
				],
			})
		);

		expect(result.success).toBe(false);
		expect(result.error).toBeTruthy();
		expect(mockPostJsonResource).not.toHaveBeenCalled();
	});
});

describe('workflowJsonWorkspace template contracts', () => {
	it('every baseName param across all samples matches the UserCreator regex', () => {
		const regex = /^[a-z0-9._-]+$/;
		let baseNameParamsChecked = 0;

		for (const sample of getWorkflowJsonSamples()) {
			const parsed = JSON.parse(sample.json) as {
				steps: Array<{
					params?: Array<{name: string; value?: unknown}>;
				}>;
			};

			for (const step of parsed.steps) {
				for (const param of step.params ?? []) {
					if (param.name === 'baseName' && typeof param.value === 'string') {
						expect(param.value, `sample ${sample.id}`).toMatch(regex);
						baseNameParamsChecked++;
					}
				}
			}
		}

		expect(baseNameParamsChecked).toBe(16);
	});
});
