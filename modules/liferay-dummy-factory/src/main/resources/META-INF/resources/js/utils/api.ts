import {ApiResponse} from '../types';
import {translate} from './i18n';

function toErrorResponse<T>(error: unknown): ApiResponse<T> {
	return {
		error: error instanceof Error ? error.message : 'Unknown error',
		success: false,
	};
}

function isNonBlankString(value: unknown): value is string {
	return typeof value === 'string' && value.trim().length > 0;
}

async function readErrorMessage(
	response: Response
): Promise<string | undefined> {
	let data;

	try {
		data = await response.json();
	}
	catch {
		return undefined; // Empty or non-JSON body: caller falls back to the HTTP status.
	}

	if (!Array.isArray(data?.errors)) {
		return undefined;
	}

	const messages = data.errors
		.map((error: {message?: unknown} | null) => error?.message)
		.filter(isNonBlankString);

	return messages.length ? messages.join('\n') : undefined;
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
	if (!response.ok) {
		const error =
			(await readErrorMessage(response)) ??
			`Server error: ${response.status}`;

		return {error, success: false};
	}

	const data = await response.json();

	if (data.success === false || data.error) {
		return {
			data,
			error: data.error || translate('execution-failed'),
			success: false,
		};
	}

	return {data, success: true};
}

export async function fetchResource<T>(
	resourceURL: string,
	params?: Record<string, string>
): Promise<ApiResponse<T>> {
	const url = new URL(resourceURL, window.location.origin);

	if (params) {
		for (const [key, value] of Object.entries(params)) {
			url.searchParams.append(key, value);
		}
	}

	try {
		const response = await fetch(url.toString(), {
			credentials: 'include',
			method: 'GET',
		});

		return await parseResponse<T>(response);
	}
	catch (error) {
		return toErrorResponse<T>(error);
	}
}

export async function postResource<T>(
	resourceURL: string,
	values: Record<string, string | number | boolean | number[]>
): Promise<ApiResponse<T>> {
	try {
		const body = new URLSearchParams();

		body.append('data', JSON.stringify(values));

		const response = await fetch(resourceURL, {
			body: body.toString(),
			credentials: 'include',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
				'x-csrf-token': Liferay.authToken,
			},
			method: 'POST',
		});

		return await parseResponse<T>(response);
	}
	catch (error) {
		return toErrorResponse<T>(error);
	}
}

export async function postJsonResource<T>(
	resourceURL: string,
	payload: unknown
): Promise<ApiResponse<T>> {
	try {
		const response = await fetch(resourceURL, {
			body: JSON.stringify(payload),
			credentials: 'include',
			headers: {
				'Content-Type': 'application/json',
				'x-csrf-token': Liferay.authToken,
			},
			method: 'POST',
		});

		return await parseResponse<T>(response);
	}
	catch (error) {
		return toErrorResponse<T>(error);
	}
}
