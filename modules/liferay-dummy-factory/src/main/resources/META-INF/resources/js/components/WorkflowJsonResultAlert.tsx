import {useEffect, useState} from 'react';

import type {WorkflowJsonWorkspaceResult} from '../utils/workflowJsonWorkspace';
import {translate} from '../utils/i18n';

export interface WorkflowJsonResultAlertProps {
	result: WorkflowJsonWorkspaceResult | null;
}

interface WorkflowJsonError {
	code?: string;
	message?: string;
	path?: string;
}

function _extractErrors(body: string): unknown[] | null {
	try {
		const parsed: unknown = JSON.parse(body);

		if (!parsed || typeof parsed !== 'object') {
			return null;
		}

		const errors = (parsed as {errors?: unknown}).errors;

		if (!Array.isArray(errors) || errors.length === 0) {
			return null;
		}

		return errors;
	}
	catch {
		return null;
	}
}

function _formatError(error: unknown): string {
	if (typeof error === 'string') {
		return error;
	}

	if (!error || typeof error !== 'object') {
		return String(error);
	}

	const {code, message, path} = error as WorkflowJsonError;
	const parts: string[] = [];

	if (code) {
		parts.push(`[${code}]`);
	}

	if (message) {
		parts.push(message);
	}

	if (path) {
		parts.push(`(${path})`);
	}

	return parts.length > 0 ? parts.join(' ') : JSON.stringify(error);
}

function WorkflowJsonResultAlert({result}: WorkflowJsonResultAlertProps) {
	const [detailsVisible, setDetailsVisible] = useState(false);

	useEffect(() => {
		setDetailsVisible(false);
	}, [result?.action, result?.tone, result?.body]);

	if (!result) {
		return null;
	}

	const toggleLabel = detailsVisible
		? translate('hide-details')
		: translate('show-details');

	const errors = _extractErrors(result.body);
	const summary =
		result.body.trim() || translate('workflow-json-no-details-returned');

	return (
		<section
			aria-live="polite"
			className={`alert alert-${result.tone}`}
			data-testid="workflow-json-result-panel"
			id="workflow-json-result-panel"
			role="status"
		>
			<div className="workflow-json-result-header">
				<span
					className={`label label-lg workflow-json-source workflow-json-source--${result.action}`}
					data-testid="workflow-json-result-source"
				>
					{result.actionLabel}
				</span>

				<h4 data-testid="workflow-json-result-title">{result.title}</h4>

				<button
					className="btn btn-sm btn-link"
					data-testid="workflow-json-result-toggle-details"
					onClick={() => setDetailsVisible((prev) => !prev)}
					type="button"
				>
					{toggleLabel}
				</button>
			</div>

			{errors ? (
				<ul
					className="workflow-json-result-errors"
					data-testid="workflow-json-result-errors"
				>
					{errors.map((error, index) => (
						<li key={index}>{_formatError(error)}</li>
					))}
				</ul>
			) : (
				<pre
					className="workflow-json-result-summary"
					data-testid="workflow-json-result-summary"
				>
					{summary}
				</pre>
			)}

			{detailsVisible && (
				<pre data-testid="workflow-json-result-body">{result.body}</pre>
			)}
		</section>
	);
}

export default WorkflowJsonResultAlert;
