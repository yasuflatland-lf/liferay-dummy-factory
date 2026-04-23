import {render, screen} from '@testing-library/react';
import {fireEvent} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import WorkflowJsonResultAlert from '../../../src/main/resources/META-INF/resources/js/components/WorkflowJsonResultAlert';
import type {WorkflowJsonWorkspaceResult} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonWorkspace';

function i18n(key: string): string {
	const text = Liferay.Language.get(key);

	expect(text).not.toBe(key);
	expect(text.length).toBeGreaterThan(0);

	return text;
}

function buildResult(
	override: Partial<WorkflowJsonWorkspaceResult> = {}
): WorkflowJsonWorkspaceResult {
	return {
		action: 'execute',
		actionLabel: 'Execute',
		body: '{"ok":true}',
		title: 'Execute completed',
		tone: 'success',
		...override,
	};
}

describe('WorkflowJsonResultAlert', () => {
	it('renders nothing when result is null', () => {
		const {container} = render(<WorkflowJsonResultAlert result={null} />);

		expect(container.firstChild).toBeNull();
	});

	it('renders alert with success tone when result.tone is success', () => {
		render(<WorkflowJsonResultAlert result={buildResult()} />);

		const panel = screen.getByTestId('workflow-json-result-panel');

		expect(panel.className).toContain('alert-success');
	});

	it('renders alert with danger tone when result.tone is danger', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({tone: 'danger', title: 'Execute failed'})}
			/>
		);

		const panel = screen.getByTestId('workflow-json-result-panel');

		expect(panel.className).toContain('alert-danger');
	});

	it('shows the title and a summary derived from the body', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({
					body: 'Short summary',
					title: 'Execute completed',
				})}
			/>
		);

		expect(
			screen.getByTestId('workflow-json-result-title').textContent
		).toBe('Execute completed');
		expect(
			screen.getByTestId('workflow-json-result-summary').textContent
		).toBe('Short summary');
	});

	it('renders the provided actionLabel rather than the raw action enum', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({action: 'plan', actionLabel: 'Plan JSON'})}
			/>
		);

		const label = screen
			.getByTestId('workflow-json-result-panel')
			.querySelector('.label');

		expect(label?.textContent).toBe('Plan JSON');
	});

	it('resets details visibility when tone changes with the same action', () => {
		const {rerender} = render(
			<WorkflowJsonResultAlert result={buildResult({tone: 'success'})} />
		);

		fireEvent.click(
			screen.getByTestId('workflow-json-result-toggle-details')
		);
		expect(
			screen.queryByTestId('workflow-json-result-body')
		).not.toBeNull();

		rerender(
			<WorkflowJsonResultAlert
				result={buildResult({tone: 'danger', title: 'Execute failed'})}
			/>
		);

		expect(screen.queryByTestId('workflow-json-result-body')).toBeNull();
	});

	it('resets details visibility when body changes with the same action', () => {
		const {rerender} = render(
			<WorkflowJsonResultAlert
				result={buildResult({body: 'first body'})}
			/>
		);

		fireEvent.click(
			screen.getByTestId('workflow-json-result-toggle-details')
		);
		expect(
			screen.queryByTestId('workflow-json-result-body')
		).not.toBeNull();

		rerender(
			<WorkflowJsonResultAlert
				result={buildResult({body: 'second body'})}
			/>
		);

		expect(screen.queryByTestId('workflow-json-result-body')).toBeNull();
	});

	it('hides details body by default', () => {
		render(<WorkflowJsonResultAlert result={buildResult()} />);

		expect(screen.queryByTestId('workflow-json-result-body')).toBeNull();
	});

	it('toggles details body when toggle button clicked', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({body: 'full body text'})}
			/>
		);

		fireEvent.click(
			screen.getByTestId('workflow-json-result-toggle-details')
		);

		expect(
			screen.getByTestId('workflow-json-result-body').textContent
		).toBe('full body text');

		fireEvent.click(
			screen.getByTestId('workflow-json-result-toggle-details')
		);

		expect(screen.queryByTestId('workflow-json-result-body')).toBeNull();
	});

	it('resets details visibility when a new result arrives', () => {
		const {rerender} = render(
			<WorkflowJsonResultAlert result={buildResult()} />
		);

		fireEvent.click(
			screen.getByTestId('workflow-json-result-toggle-details')
		);
		expect(
			screen.queryByTestId('workflow-json-result-body')
		).not.toBeNull();

		rerender(
			<WorkflowJsonResultAlert
				result={buildResult({action: 'plan', title: 'Plan completed'})}
			/>
		);

		expect(screen.queryByTestId('workflow-json-result-body')).toBeNull();
	});

	it('uses hide-details and show-details i18n keys for the toggle label', () => {
		const showText = i18n('show-details');
		const hideText = i18n('hide-details');

		render(<WorkflowJsonResultAlert result={buildResult()} />);

		const toggle = screen.getByTestId(
			'workflow-json-result-toggle-details'
		);

		expect(toggle.textContent).toBe(showText);

		fireEvent.click(toggle);

		expect(toggle.textContent).toBe(hideText);
	});
});

describe('WorkflowJsonResultAlert a11y', () => {
	it('exposes role=status and aria-live=polite', () => {
		render(
			<WorkflowJsonResultAlert
				result={{
					action: 'execute',
					actionLabel: 'Execute',
					body: '{}',
					title: 'Done',
					tone: 'success',
				}}
			/>
		);

		const panel = screen.getByTestId('workflow-json-result-panel');

		expect(panel.getAttribute('role')).toBe('status');
		expect(panel.getAttribute('aria-live')).toBe('polite');
	});

	it('renders an Ajv source badge when action is "ajv"', () => {
		const ajvLabel = i18n('workflow-json-source-ajv');

		render(
			<WorkflowJsonResultAlert
				result={buildResult({
					action: 'ajv',
					actionLabel: ajvLabel,
					tone: 'danger',
				})}
			/>
		);

		const badge = screen.getByTestId('workflow-json-result-source');

		expect(badge.textContent).toBe(ajvLabel);
		expect(badge.className).toContain('workflow-json-source--ajv');
	});

	it('renders a Plan source badge when action is "plan"', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({
					action: 'plan',
					actionLabel: 'Plan',
				})}
			/>
		);

		const badge = screen.getByTestId('workflow-json-result-source');

		expect(badge.className).toContain('workflow-json-source--plan');
	});

	it('renders an Execute source badge when action is "execute"', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({
					action: 'execute',
					actionLabel: 'Execute',
				})}
			/>
		);

		const badge = screen.getByTestId('workflow-json-result-source');

		expect(badge.className).toContain('workflow-json-source--execute');
	});

	it('renders a Load source badge when action is "load"', () => {
		render(
			<WorkflowJsonResultAlert
				result={buildResult({
					action: 'load',
					actionLabel: 'Load',
				})}
			/>
		);

		const badge = screen.getByTestId('workflow-json-result-source');

		expect(badge.className).toContain('workflow-json-source--load');
	});
});
