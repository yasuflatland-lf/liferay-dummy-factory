import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
	fetchResource,
	postJsonResource,
	postResource,
} from '../../src/main/resources/META-INF/resources/js/utils/api';

const mockFetch = vi.fn();

beforeEach(() => {
	global.fetch = mockFetch;
	mockFetch.mockReset();
});

describe.each([
	['fetchResource', () => fetchResource('/api/resource')],
	['postResource', () => postResource('/api/resource', {})],
	['postJsonResource', () => postJsonResource('/o/ldf-workflow/execute', {})],
] as const)('%s responses', (_name, request) => {
	it('resolves with an error for a 200 non-JSON body', async () => {
		mockFetch.mockResolvedValueOnce(new Response('<html>', {status: 200}));

		await expect(request()).resolves.toEqual({
			error: expect.any(String),
			success: false,
		});
	});

	it('surfaces a 403 authorization message', async () => {
		mockFetch.mockResolvedValueOnce(
			new Response(
				JSON.stringify({
					errors: [
						{
							code: 'FORBIDDEN',
							message: 'company.create requires an omniadmin.',
							path: '/',
						},
					],
				}),
				{status: 403}
			)
		);

		expect(await request()).toEqual({
			error: 'company.create requires an omniadmin.',
			success: false,
		});
	});

	it('joins usable messages with newlines and ignores malformed entries', async () => {
		mockFetch.mockResolvedValueOnce(
			new Response(
				JSON.stringify({
					errors: [
						{message: 'First error'},
						null,
						{},
						{message: 42},
						{message: ''},
						{message: ' \t '},
						{message: 'Second error'},
					],
				}),
				{status: 400}
			)
		);

		expect(await request()).toEqual({
			error: 'First error\nSecond error',
			success: false,
		});
	});

	it.each([
		['non-JSON body', '<html>Forbidden</html>'],
		['empty body', ''],
		['JSON without errors', JSON.stringify({message: 'Forbidden'})],
		['empty errors', JSON.stringify({errors: []})],
		[
			'unusable messages',
			JSON.stringify({errors: [null, {}, {message: 42}, {message: ' \t '}]}),
		],
		['non-array errors', JSON.stringify({errors: {message: 'Forbidden'}})],
		['null JSON', 'null'],
	])('falls back to the HTTP status for %s', async (_description, body) => {
		mockFetch.mockResolvedValueOnce(new Response(body, {status: 403}));

		expect(await request()).toEqual({
			error: 'Server error: 403',
			success: false,
		});
	});
});

describe('postResource', () => {
	it('sends POST with JSON-stringified data in data parameter', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({result: 42}),
			ok: true,
		});

		await postResource('/api/resource', {
			num1: '10',
			num2: '32',
			operator: '+',
		});

		expect(mockFetch).toHaveBeenCalledWith('/api/resource', {
			body: 'data=%7B%22num1%22%3A%2210%22%2C%22num2%22%3A%2232%22%2C%22operator%22%3A%22%2B%22%7D',
			credentials: 'include',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
				'x-csrf-token': 'test-auth-token',
			},
			method: 'POST',
		});
	});

	it('includes x-csrf-token header with Liferay.authToken', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({result: 1}),
			ok: true,
		});

		await postResource('/api/resource', {key: 'value'});

		const callHeaders = mockFetch.mock.calls[0][1].headers;

		expect(callHeaders['x-csrf-token']).toBe('test-auth-token');
	});

	it('includes Content-Type application/x-www-form-urlencoded', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({result: 1}),
			ok: true,
		});

		await postResource('/api/resource', {key: 'value'});

		const callHeaders = mockFetch.mock.calls[0][1].headers;

		expect(callHeaders['Content-Type']).toBe(
			'application/x-www-form-urlencoded'
		);
	});

	it('returns success true with data on successful response', async () => {
		const responseData = {result: 42};

		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve(responseData),
			ok: true,
		});

		const result = await postResource('/api/resource', {num1: '10'});

		expect(result).toEqual({data: responseData, success: true});
	});

	it('returns success false with error when response has error field', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({error: 'Division by zero'}),
			ok: true,
		});

		const result = await postResource('/api/resource', {num1: '10'});

		expect(result).toEqual({
			data: {error: 'Division by zero'},
			error: 'Division by zero',
			success: false,
		});
	});

	it('returns success false with server error when response.ok is false', async () => {
		mockFetch.mockResolvedValueOnce(new Response('', {status: 500}));

		const result = await postResource('/api/resource', {num1: '10'});

		expect(result).toEqual({error: 'Server error: 500', success: false});
	});

	it('classifies {success: false} without error field as failure', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () =>
				Promise.resolve({count: 3, requested: 5, success: false}),
			ok: true,
		});

		const result = await postResource('/api/resource', {num1: '10'});

		expect(result.success).toBe(false);

		if (!result.success) {
			expect(result.error).not.toBe('execution-failed');
			expect(result.error).toBe('Execution Failed');
			expect(result.data).toEqual({
				count: 3,
				requested: 5,
				success: false,
			});
		}
	});
});

describe('fetchResource', () => {
	it('sends GET with query parameters', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({items: []}),
			ok: true,
		});

		await fetchResource('http://localhost/api/resource', {
			page: '1',
			size: '10',
		});

		const calledUrl = mockFetch.mock.calls[0][0];

		expect(calledUrl).toContain('page=1');
		expect(calledUrl).toContain('size=10');
		expect(mockFetch.mock.calls[0][1]).toEqual({
			credentials: 'include',
			method: 'GET',
		});
	});

	it('returns success true with data on successful response', async () => {
		const responseData = {items: ['a', 'b']};

		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve(responseData),
			ok: true,
		});

		const result = await fetchResource('http://localhost/api/resource');

		expect(result).toEqual({data: responseData, success: true});
	});

	it('returns success false with error when response has error field', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({error: 'Not found'}),
			ok: true,
		});

		const result = await fetchResource('http://localhost/api/resource');

		expect(result).toEqual({
			data: {error: 'Not found'},
			error: 'Not found',
			success: false,
		});
	});

	it('returns success false with server error when response.ok is false', async () => {
		mockFetch.mockResolvedValueOnce(new Response('', {status: 404}));

		const result = await fetchResource('http://localhost/api/resource');

		expect(result).toEqual({error: 'Server error: 404', success: false});
	});

	it('classifies {success: false} without error field as failure', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({items: [], success: false}),
			ok: true,
		});

		const result = await fetchResource('http://localhost/api/resource');

		expect(result.success).toBe(false);

		if (!result.success) {
			expect(result.error).not.toBe('execution-failed');
			expect(result.error).toBe('Execution Failed');
			expect(result.data).toEqual({items: [], success: false});
		}
	});
});
