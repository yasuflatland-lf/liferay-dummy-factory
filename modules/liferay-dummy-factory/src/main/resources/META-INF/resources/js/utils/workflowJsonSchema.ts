import Ajv2020, {ValidateFunction} from 'ajv/dist/2020';

import {WorkflowRequestPayload} from '../types';

export interface WorkflowJsonValidationIssue {
	message: string;
	path: string;
}

export type WorkflowJsonValidationResult =
	| {kind: 'empty'}
	| {
			errors: WorkflowJsonValidationIssue[];
			kind: 'invalid';
	  }
	| {kind: 'ok'; value: WorkflowRequestPayload};

const _SCHEMA_URL = '/o/ldf-workflow/schema';

let _validatorPromise: Promise<ValidateFunction | null> | null = null;
let _schemaError: string | null = null;

function _initValidator(): Promise<ValidateFunction | null> {
	if (_validatorPromise) {
		return _validatorPromise;
	}

	_validatorPromise = fetch(_SCHEMA_URL, {
		credentials: 'include',
		headers: {
			'x-csrf-token': Liferay.authToken,
		},
	})
		.then((res) => {
			if (!res.ok) {
				throw new Error(`HTTP ${res.status}`);
			}

			return res.json();
		})
		.then((doc) => {
			if (!doc?.schema) {
				throw new Error(
					'Schema response missing .schema property'
				);
			}

			return new Ajv2020({allErrors: true}).compile(doc.schema);
		})
		.catch((err: unknown) => {
			_schemaError =
				err instanceof Error ? err.message : String(err);
			console.error(
				'[workflowJsonSchema] Failed to load schema:',
				err
			);

			return null;
		});

	return _validatorPromise;
}

export function initSchema(): Promise<void> {
	return _initValidator().then(() => undefined);
}

export function schemaError(): string | null {
	return _schemaError;
}

export function _resetSchemaForTest(): void {
	_validatorPromise = null;
	_schemaError = null;
}

export async function validateWorkflowJsonText(
	jsonText: string
): Promise<WorkflowJsonValidationResult> {
	if (jsonText.trim() === '') {
		return {kind: 'empty'};
	}

	const validate = await _initValidator();

	if (!validate) {
		return {
			errors: [
				{
					message: _schemaError ?? 'Schema unavailable',
					path: '/',
				},
			],
			kind: 'invalid',
		};
	}

	let parsed: unknown;

	try {
		parsed = JSON.parse(jsonText);
	}
	catch (error) {
		return {
			errors: [
				{
					message:
						error instanceof Error ? error.message : String(error),
					path: '',
				},
			],
			kind: 'invalid',
		};
	}

	if (!validate(parsed)) {
		return {
			errors: (validate.errors ?? []).map((ajvError) => ({
				message: ajvError.message ?? 'is invalid',
				path: ajvError.instancePath || '/',
			})),
			kind: 'invalid',
		};
	}

	return {kind: 'ok', value: parsed as WorkflowRequestPayload};
}
