import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
	_resetSchemaForTest,
	validateWorkflowJsonText,
} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonSchema';

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

const VALID_JSON = JSON.stringify({
	schemaVersion: '1.0',
	steps: [
		{
			id: 'a',
			idempotencyKey: 'x',
			operation: 'noop',
		},
	],
	workflowId: 't',
});

beforeEach(() => {
	_resetSchemaForTest();
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

describe('validateWorkflowJsonText', () => {
	it('returns kind=empty for empty string', async () => {
		expect(await validateWorkflowJsonText('')).toEqual({kind: 'empty'});
	});

	it('returns kind=empty for whitespace-only string', async () => {
		expect(await validateWorkflowJsonText('   \n\t')).toEqual({
			kind: 'empty',
		});
	});

	it('returns kind=invalid for malformed JSON', async () => {
		const result = await validateWorkflowJsonText('{"not valid');

		expect(result.kind).toBe('invalid');

		if (result.kind === 'invalid') {
			expect(result.errors.length).toBeGreaterThan(0);
			expect(result.errors[0]).toHaveProperty('path');
			expect(result.errors[0]).toHaveProperty('message');
			expect(typeof result.errors[0].path).toBe('string');
			expect(typeof result.errors[0].message).toBe('string');
		}
	});

	it('returns kind=invalid when schemaVersion is missing', async () => {
		const result = await validateWorkflowJsonText(
			JSON.stringify({
				steps: [{id: 'a', idempotencyKey: 'x', operation: 'noop'}],
			})
		);

		expect(result.kind).toBe('invalid');
	});

	it('returns kind=invalid when steps is empty', async () => {
		const result = await validateWorkflowJsonText(
			JSON.stringify({schemaVersion: '1.0', steps: []})
		);

		expect(result.kind).toBe('invalid');
	});

	it('returns kind=invalid when a step is missing required fields', async () => {
		const result = await validateWorkflowJsonText(
			JSON.stringify({
				schemaVersion: '1.0',
				steps: [{id: 'a'}],
			})
		);

		expect(result.kind).toBe('invalid');
	});

	it('returns kind=invalid when a param lacks both value and from', async () => {
		const result = await validateWorkflowJsonText(
			JSON.stringify({
				schemaVersion: '1.0',
				steps: [
					{
						id: 'a',
						idempotencyKey: 'x',
						operation: 'noop',
						params: [{name: 'p'}],
					},
				],
			})
		);

		expect(result.kind).toBe('invalid');
	});

	it('returns kind=ok for a minimal valid payload', async () => {
		const result = await validateWorkflowJsonText(VALID_JSON);

		expect(result.kind).toBe('ok');

		if (result.kind === 'ok') {
			expect(result.value.schemaVersion).toBe('1.0');
			expect(result.value.steps).toHaveLength(1);
		}
	});

	it('every invalid error has string path and string message (deterministic lock)', async () => {
		const result = await validateWorkflowJsonText('{}');

		expect(result.kind).toBe('invalid');

		if (result.kind === 'invalid') {
			expect(
				result.errors.every(
					(error) =>
						typeof error.path === 'string' &&
						typeof error.message === 'string'
				)
			).toBe(true);
		}
	});

	it('returns kind=invalid when schema fetch fails', async () => {
		_resetSchemaForTest();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockRejectedValue(new Error('Network error'))
		);

		const result = await validateWorkflowJsonText(
			JSON.stringify({
				schemaVersion: '1.0',
				steps: [
					{id: 'a', idempotencyKey: 'x', operation: 'noop'},
				],
			})
		);

		expect(result.kind).toBe('invalid');

		if (result.kind === 'invalid') {
			expect(result.errors).toHaveLength(1);
			expect(result.errors[0].message).toBeTruthy();
			expect(result.errors[0].path).toBe('/');
		}
	});

	it('fetches schema only once for multiple validation calls (singleton)', async () => {
		await validateWorkflowJsonText(VALID_JSON);
		await validateWorkflowJsonText(VALID_JSON);

		expect(
			(global.fetch as ReturnType<typeof vi.fn>).mock.calls
		).toHaveLength(1);
	});

	it('sends credentials and x-csrf-token header so the Liferay auth filter accepts the request', async () => {
		await validateWorkflowJsonText(VALID_JSON);

		const [, init] = (global.fetch as ReturnType<typeof vi.fn>).mock
			.calls[0];

		expect(init?.credentials).toBe('include');
		expect(init?.headers?.['x-csrf-token']).toBe(Liferay.authToken);
	});

	it('returns kind=invalid when schema endpoint returns HTTP error', async () => {
		_resetSchemaForTest();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockResolvedValue({
				json: async () => ({}),
				ok: false,
				status: 404,
			})
		);

		const result = await validateWorkflowJsonText(VALID_JSON);

		expect(result.kind).toBe('invalid');

		if (result.kind === 'invalid') {
			expect(result.errors[0].message).toContain('404');
		}
	});

	it('returns kind=invalid when schema response is missing .schema property', async () => {
		_resetSchemaForTest();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockResolvedValue({
				json: async () => ({}),
				ok: true,
			})
		);

		const result = await validateWorkflowJsonText(VALID_JSON);

		expect(result.kind).toBe('invalid');
	});
});
