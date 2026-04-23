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
