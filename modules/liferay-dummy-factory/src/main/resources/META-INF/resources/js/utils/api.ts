import {ApiResponse} from '../types';
import {translate} from './i18n';

function toErrorResponse<T>(error: unknown): ApiResponse<T> {
	return {
		error: error instanceof Error ? error.message : 'Unknown error',
		success: false,
	};
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
	if (!response.ok) {
		try {
			const data = await response.json();
			const messages = Array.isArray(data?.errors)
				? data.errors
						.map((error: {message?: unknown} | null) => error?.message)
						.filter(
							(message: unknown): message is string =>
								typeof message === 'string' && message.trim().length > 0
						)
				: [];

			if (messages.length) {
				return {error: messages.join('\n'), success: false};
			}
		}
		catch {
			// Empty or non-JSON error bodies use the HTTP status fallback.
		}

		return {error: `Server error: ${response.status}`, success: false};
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

		return parseResponse<T>(response);
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

		return parseResponse<T>(response);
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

		return parseResponse<T>(response);
	}
	catch (error) {
		return toErrorResponse<T>(error);
	}
}
