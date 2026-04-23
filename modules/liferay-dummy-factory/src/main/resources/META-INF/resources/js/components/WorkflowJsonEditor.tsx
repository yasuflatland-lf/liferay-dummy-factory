import {useEffect, useMemo, useRef, useState} from 'react';

import {translate} from '../utils/i18n';
import {
	initSchema,
	schemaError,
	validateWorkflowJsonText,
} from '../utils/workflowJsonSchema';
import {
	executeWorkflowJson,
	getWorkflowJsonSamples,
	loadWorkflowJsonSample,
	planWorkflowJson,
	type WorkflowJsonResultTone,
	type WorkflowJsonSample,
	type WorkflowJsonWorkspaceResult,
} from '../utils/workflowJsonWorkspace';
import WorkflowJsonProgressBar from './WorkflowJsonProgressBar';
import WorkflowJsonResultAlert from './WorkflowJsonResultAlert';

export interface WorkflowJsonEditorProps {
	executeResourceURL?: string;
	onChange: (value: string) => void;
	planResourceURL?: string;
	value: string;
}

const _workflowJsonSamples = getWorkflowJsonSamples();

function _stringifyValue(value: unknown): string {
	if (typeof value === 'string') {
		return value;
	}

	try {
		return JSON.stringify(value, null, 2);
	} catch {
		return String(value);
	}
}

function _getActionLabel(
	action: WorkflowJsonWorkspaceResult['action']
): string {
	switch (action) {
		case 'ajv':
			return translate('workflow-json-source-ajv');
		case 'execute':
			return translate('execute-json');
		case 'load':
			return translate('load-sample');
		case 'plan':
			return translate('plan-json');
	}
}

function _getResponseBody(response: unknown): string {
	if (!response) {
		return translate('workflow-json-no-data-returned');
	}

	return _stringifyValue(response);
}

function _hasValidationErrors(data: unknown): boolean {
	if (!data || typeof data !== 'object' || Array.isArray(data)) {
		return false;
	}

	const candidate = data as {errors?: unknown};

	return Array.isArray(candidate.errors) && candidate.errors.length > 0;
}

function _getResultTone(
	error: boolean,
	hasWarnings: boolean
): WorkflowJsonResultTone {
	if (error) {
		return 'danger';
	}

	if (hasWarnings) {
		return 'warning';
	}

	return 'success';
}

function _format(template: string, ...args: string[]): string {
	return args.reduce(
		(current, arg, index) => current.replace(`{${index}}`, arg),
		template
	);
}

function _createResult(
	action: WorkflowJsonWorkspaceResult['action'],
	title: string,
	body: string,
	tone: WorkflowJsonResultTone
): WorkflowJsonWorkspaceResult {
	return {
		action,
		actionLabel: _getActionLabel(action),
		body,
		title,
		tone,
	};
}

function _ajvResultFromOutcome(
	outcome:
		| {kind: 'empty'}
		| {
				errors: {path: string; message: string}[];
				kind: 'invalid';
		  }
): WorkflowJsonWorkspaceResult {
	const title =
		outcome.kind === 'empty'
			? translate('workflow-json-empty-error')
			: translate('workflow-json-invalid-error');

	const body =
		outcome.kind === 'empty'
			? translate('workflow-json-empty-error')
			: JSON.stringify({errors: outcome.errors}, null, 2);

	return {
		action: 'ajv',
		actionLabel: translate('workflow-json-source-ajv'),
		body,
		title,
		tone: 'danger',
	};
}

function WorkflowJsonEditor({
	executeResourceURL,
	onChange,
	planResourceURL,
	value,
}: WorkflowJsonEditorProps) {
	const [draftValue, setDraftValue] = useState(value);
	const [busyAction, setBusyAction] = useState<
		WorkflowJsonWorkspaceResult['action'] | null
	>(null);
	const [liveValidity, setLiveValidity] = useState<
		'empty' | 'invalid' | 'ok'
	>('ok');
	const [result, setResult] = useState<WorkflowJsonWorkspaceResult | null>(
		null
	);
	const [selectedSampleId, setSelectedSampleId] = useState(
		_workflowJsonSamples[0]?.id ?? ''
	);
	const [schemaStatus, setSchemaStatus] = useState<
		'error' | 'loading' | 'ready'
	>('loading');
	const _debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

	useEffect(() => {
		setDraftValue(value);
	}, [value]);

	useEffect(() => {
		initSchema().then(() => {
			setSchemaStatus(schemaError() !== null ? 'error' : 'ready');
		});
	}, []);

	useEffect(() => {
		if (_debounceRef.current) {
			clearTimeout(_debounceRef.current);
		}

		let cancelled = false;

		_debounceRef.current = setTimeout(async () => {
			const outcome = await validateWorkflowJsonText(draftValue);

			if (!cancelled) {
				setLiveValidity(outcome.kind);
			}
		}, 300);

		return () => {
			cancelled = true;

			if (_debounceRef.current) {
				clearTimeout(_debounceRef.current);
			}
		};
	}, [draftValue]);

	const selectedSample = useMemo<WorkflowJsonSample | null>(() => {
		return (
			_workflowJsonSamples.find(
				(sample) => sample.id === selectedSampleId
			) ??
			_workflowJsonSamples[0] ??
			null
		);
	}, [selectedSampleId]);

	function _applyValue(nextValue: string) {
		setDraftValue(nextValue);
		onChange(nextValue);

		if (result !== null && result.action === 'ajv') {
			setResult(null);
		}
	}

	function _setResult(
		action: WorkflowJsonWorkspaceResult['action'],
		title: string,
		body: string,
		tone: WorkflowJsonResultTone
	) {
		setResult(_createResult(action, title, body, tone));
	}

	function _handleLoadSample() {
		if (!selectedSample) {
			_setResult(
				'load',
				translate('workflow-json-sample-not-available'),
				translate('workflow-json-no-sample-selected'),
				'danger'
			);
			return;
		}

		const json = loadWorkflowJsonSample(selectedSample.id);
		const title = translate(selectedSample.titleKey);

		if (!json) {
			_setResult(
				'load',
				translate('workflow-json-sample-not-available'),
				_format(translate('workflow-json-sample-load-failed'), title),
				'danger'
			);
			return;
		}

		_applyValue(json);
		_setResult(
			'load',
			_format(translate('workflow-json-sample-loaded-title'), title),
			_format(
				translate('workflow-json-sample-loaded-body'),
				selectedSample.id
			),
			'success'
		);
	}

	async function _handleWorkflowAction(
		action: 'execute' | 'plan',
		resourceURL?: string
	) {
		const outcome = await validateWorkflowJsonText(draftValue);

		if (outcome.kind !== 'ok') {
			setResult(_ajvResultFromOutcome(outcome));
			return;
		}

		const actionLabel = _getActionLabel(action);

		if (!resourceURL) {
			_setResult(
				action,
				_format(
					translate('workflow-json-action-unavailable'),
					actionLabel
				),
				_format(
					translate('workflow-json-action-endpoint-missing'),
					actionLabel
				),
				'danger'
			);
			return;
		}

		setBusyAction(action);

		try {
			const response =
				action === 'execute'
					? await executeWorkflowJson(resourceURL, draftValue)
					: await planWorkflowJson(resourceURL, draftValue);

			if (!response.success) {
				_setResult(
					action,
					_format(
						translate('workflow-json-action-failed'),
						actionLabel
					),
					response.error,
					'danger'
				);
				return;
			}

			const hasWarnings = _hasValidationErrors(response.data);
			const titleKey = hasWarnings
				? 'workflow-json-action-completed-with-warnings'
				: 'workflow-json-action-completed';

			_setResult(
				action,
				_format(translate(titleKey), actionLabel),
				_getResponseBody(response.data),
				_getResultTone(false, hasWarnings)
			);
		} catch (error) {
			console.error('Workflow action failed unexpectedly', error);

			const message =
				error instanceof Error ? error.message : String(error);

			_setResult(
				action,
				_format(translate('workflow-json-action-failed'), actionLabel),
				message || translate('workflow-json-no-details-returned'),
				'danger'
			);
		} finally {
			setBusyAction(null);
		}
	}

	const schemaReady = schemaStatus === 'ready';
	const canPlan = Boolean(planResourceURL) && schemaReady;
	const canExecute = Boolean(executeResourceURL) && schemaReady;
	const isBusy = busyAction !== null;

	return (
		<div aria-busy={isBusy} className="workflow-json-workspace">
			{schemaStatus === 'error' && (
				<div className="alert alert-danger" role="alert">
					{translate('workflow-json-schema-unavailable')}
				</div>
			)}

			<div
				aria-label={translate('workflow-json')}
				className="workflow-json-toolbar"
				data-testid="workflow-json-toolbar"
				role="toolbar"
			>
				<div className="workflow-json-toolbar-left">
					<span className="workflow-json-toolbar-title">
						{translate('workflow-json')}
					</span>

					<span className="workflow-json-toolbar-tag">
						{translate('workflow-json-console-tag')}
					</span>
				</div>

				<div className="workflow-json-toolbar-right">
					<button
						className="btn btn-secondary"
						data-testid="workflow-json-plan"
						disabled={!canPlan || isBusy}
						onClick={() =>
							_handleWorkflowAction('plan', planResourceURL)
						}
						type="button"
					>
						{translate('plan-json')}
					</button>

					<button
						className="btn btn-primary"
						data-testid="workflow-json-execute"
						disabled={!canExecute || isBusy}
						onClick={() =>
							_handleWorkflowAction('execute', executeResourceURL)
						}
						type="button"
					>
						{translate('execute-json')}
					</button>
				</div>
			</div>

			<WorkflowJsonProgressBar busy={isBusy} />

			<section className="sheet">
				<div className="sheet-section">
					<div className="form-group">
						<label htmlFor="workflow-json-sample-select">
							{translate('workflow-json-sample-workflow-label')}
						</label>

						<div className="form-inline">
							<select
								className="form-control mr-3"
								data-testid="workflow-json-sample-select"
								id="workflow-json-sample-select"
								onChange={(event) =>
									setSelectedSampleId(event.target.value)
								}
								value={selectedSample?.id ?? ''}
							>
								{_workflowJsonSamples.map((sample) => (
									<option key={sample.id} value={sample.id}>
										{translate(sample.titleKey)}
									</option>
								))}
							</select>

							<button
								className="btn btn-secondary"
								data-testid="workflow-json-load-sample"
								disabled={!selectedSample || isBusy}
								onClick={_handleLoadSample}
								type="button"
							>
								{translate('load-sample')}
							</button>
						</div>

						{selectedSample &&
							selectedSample.operations.length > 0 && (
								<div className="workflow-json-sample-operations">
									{selectedSample.operations.map((operation) => (
										<span
											className="label label-secondary"
											key={operation}
										>
											{operation}
										</span>
									))}
								</div>
							)}
					</div>
				</div>

				<div className="sheet-section workflow-json-split">
					<div className="workflow-json-pane workflow-json-pane--editor">
						<label
							className="workflow-json-pane-label"
							htmlFor="workflow-json-editor"
						>
							{translate('workflow-json-editor')}
						</label>

						<textarea
							aria-describedby={
								result !== null && result.action === 'ajv'
									? 'workflow-json-result-panel'
									: undefined
							}
							aria-invalid={
								liveValidity === 'invalid' ||
								(result !== null && result.action === 'ajv')
							}
							className={`form-control workflow-json-textarea${
								liveValidity === 'invalid' ? ' is-invalid' : ''
							}`}
							data-testid="workflow-json-textarea"
							id="workflow-json-editor"
							onChange={(event) => _applyValue(event.target.value)}
							placeholder={translate(
								'workflow-json-editor-placeholder'
							)}
							readOnly={isBusy}
							spellCheck={false}
							value={draftValue}
						/>
					</div>

					<div className="workflow-json-pane workflow-json-pane--result">
						{result ? (
							<WorkflowJsonResultAlert result={result} />
						) : (
							<div
								className="workflow-json-pane-empty"
								data-testid="workflow-json-result-placeholder"
							>
								{translate('workflow-json-result-placeholder')}
							</div>
						)}
					</div>
				</div>
			</section>
		</div>
	);
}

export default WorkflowJsonEditor;
