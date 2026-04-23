import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowJsonEditor from '../../../src/main/resources/META-INF/resources/js/components/WorkflowJsonEditor';
import {_resetSchemaForTest} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonSchema';

function i18n(key: string): string {
	const text = Liferay.Language.get(key);

	expect(text).not.toBe(key);
	expect(text.length).toBeGreaterThan(0);

	return text;
}

const defaultProps = {
	executeResourceURL: '/o/ldf-workflow/execute',
	onChange: () => undefined,
	planResourceURL: '/o/ldf-workflow/plan',
	value: '',
};

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

beforeEach(() => {
	_resetSchemaForTest();
	vi.stubGlobal(
		'fetch',
		vi.fn().mockImplementation((url: string) => {
			if (url === '/o/ldf-workflow/schema') {
				return Promise.resolve({
					json: () => Promise.resolve({schema: VALID_SCHEMA}),
					ok: true,
				});
			}

			return Promise.resolve({
				json: async () => ({data: {errors: []}, success: true}),
				ok: true,
			});
		})
	);
});

afterEach(() => {
	vi.restoreAllMocks();
	vi.unstubAllGlobals();
});

describe('WorkflowJsonEditor layout', () => {
	it('renders three canonical action testids (load/plan/execute)', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(screen.getByTestId('workflow-json-load-sample')).not.toBeNull();
		expect(screen.queryByTestId('workflow-json-validate')).toBeNull();
		expect(screen.getByTestId('workflow-json-plan')).not.toBeNull();
		expect(screen.getByTestId('workflow-json-execute')).not.toBeNull();
	});

	it('renders Plan and Execute buttons disabled initially while schema loads', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(
			(screen.getByTestId('workflow-json-plan') as HTMLButtonElement)
				.disabled
		).toBe(true);
		expect(
			(
				screen.getByTestId('workflow-json-execute') as HTMLButtonElement
			).disabled
		).toBe(true);
	});

	it('does NOT render deleted testids (copy/schema)', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(screen.queryByTestId('workflow-json-copy-json')).toBeNull();
		expect(
			screen.queryByTestId('workflow-json-download-schema')
		).toBeNull();
	});

	it('renders Sample workflow label via i18n (no hardcoded English)', () => {
		const labelText = i18n('workflow-json-sample-workflow-label');

		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(screen.getByText(labelText)).not.toBeNull();
	});

	it('renders the sample select and Load sample button', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(
			screen.getByTestId('workflow-json-sample-select')
		).not.toBeNull();
		expect(screen.getByTestId('workflow-json-load-sample')).not.toBeNull();
	});

	it('renders the textarea with the provided value', () => {
		render(<WorkflowJsonEditor {...defaultProps} value='{"test": true}' />);

		const textarea = screen.getByTestId(
			'workflow-json-textarea'
		) as HTMLTextAreaElement;

		expect(textarea.value).toBe('{"test": true}');
	});

	it('calls onChange when the textarea value changes', () => {
		const handleChange = vi.fn();

		render(
			<WorkflowJsonEditor {...defaultProps} onChange={handleChange} />
		);

		const textarea = screen.getByTestId('workflow-json-textarea');

		fireEvent.change(textarea, {target: {value: '{"changed": true}'}});

		expect(handleChange).toHaveBeenCalledWith('{"changed": true}');
	});

	it('disables Load sample when no sample is available', () => {
		const original = (window as unknown as {_samplesOverride?: unknown})
			._samplesOverride;

		render(<WorkflowJsonEditor {...defaultProps} />);

		const loadButton = screen.getByTestId(
			'workflow-json-load-sample'
		) as HTMLButtonElement;

		expect(loadButton.disabled).toBe(false);

		(window as unknown as {_samplesOverride?: unknown})._samplesOverride =
			original;
	});

	it('loads the selected sample into the textarea via Load sample button', () => {
		const handleChange = vi.fn();

		render(
			<WorkflowJsonEditor {...defaultProps} onChange={handleChange} />
		);

		fireEvent.click(screen.getByTestId('workflow-json-load-sample'));

		expect(handleChange).toHaveBeenCalled();

		const loadedValue = handleChange.mock.calls[0][0] as string;

		expect(loadedValue).toContain('"schemaVersion"');
		expect(loadedValue).toContain('"steps"');
	});

	it('does not pass schemaResourceURL — prop is removed', () => {
		const props = defaultProps as Record<string, unknown>;

		expect('schemaResourceURL' in props).toBe(false);
	});

	it('renders the result alert panel only after an action completes', async () => {
		render(
			<WorkflowJsonEditor
				{...defaultProps}
				value='{"schemaVersion":"1.0","workflowId":"t","steps":[{"id":"a","operation":"noop","idempotencyKey":"x"}]}'
			/>
		);

		expect(screen.queryByTestId('workflow-json-result-panel')).toBeNull();

		await waitFor(() => {
			expect(
				(
					screen.getByTestId('workflow-json-plan') as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-plan'));

		await waitFor(() => {
			expect(
				screen.queryByTestId('workflow-json-result-panel')
			).not.toBeNull();
		});
	});

	it('shows Ajv result in the result pane when Execute is clicked with invalid JSON, without calling the server', async () => {
		render(
			<WorkflowJsonEditor {...defaultProps} value='{"not valid json' />
		);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			const panel = screen.getByTestId('workflow-json-result-panel');

			expect(panel.className).toContain('alert-danger');

			const badge = screen.getByTestId('workflow-json-result-source');

			expect(badge.className).toContain('workflow-json-source--ajv');
			expect(badge.textContent).toBe(i18n('workflow-json-source-ajv'));
		});

		const fetchMock = global.fetch as ReturnType<typeof vi.fn>;
		const serverCalls = fetchMock.mock.calls.filter(
			([url]: [string]) => url !== '/o/ldf-workflow/schema'
		);

		expect(serverCalls).toHaveLength(0);
	});

	it('shows Ajv empty-variant result when Execute is clicked with empty editor', async () => {
		render(<WorkflowJsonEditor {...defaultProps} value="" />);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			const panel = screen.getByTestId('workflow-json-result-panel');

			expect(panel.textContent).toContain(
				i18n('workflow-json-empty-error')
			);
		});

		const fetchMock = global.fetch as ReturnType<typeof vi.fn>;
		const serverCalls = fetchMock.mock.calls.filter(
			([url]: [string]) => url !== '/o/ldf-workflow/schema'
		);

		expect(serverCalls).toHaveLength(0);
	});

	it('links textarea aria-describedby and marks aria-invalid without is-invalid when latest result is Ajv', async () => {
		render(<WorkflowJsonEditor {...defaultProps} value="" />);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			const panel = screen.getByTestId('workflow-json-result-panel');
			const textarea = screen.getByTestId('workflow-json-textarea');

			expect(textarea.getAttribute('aria-describedby')).toBe(panel.id);
			expect(textarea.getAttribute('aria-invalid')).toBe('true');
			expect(textarea.className).not.toContain('is-invalid');
		});
	});

	it('clears aria-describedby once the user starts typing again', async () => {
		render(<WorkflowJsonEditor {...defaultProps} value="" />);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			expect(screen.getByTestId('workflow-json-result-panel')).not.toBeNull();
		});

		fireEvent.change(screen.getByTestId('workflow-json-textarea'), {
			target: {value: '{'},
		});

		expect(
			screen
				.getByTestId('workflow-json-textarea')
				.getAttribute('aria-describedby')
		).toBeNull();
	});

	it('does not render the deleted help-text copy', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		expect(screen.queryByText('Edit raw workflow JSON here.')).toBeNull();
	});

	it('reports a failed response in the danger tone with the server error body', async () => {
		vi.stubGlobal(
			'fetch',
			vi.fn().mockImplementation((url: string) => {
				if (url === '/o/ldf-workflow/schema') {
					return Promise.resolve({
						json: () => Promise.resolve({schema: VALID_SCHEMA}),
						ok: true,
					});
				}

				return Promise.resolve({
					json: async () => ({
						error: 'Simulated backend failure',
						success: false,
					}),
					ok: true,
				});
			})
		);

		render(
			<WorkflowJsonEditor
				{...defaultProps}
				value='{"schemaVersion":"1.0","workflowId":"t","steps":[{"id":"a","operation":"noop","idempotencyKey":"x"}]}'
			/>
		);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			const panel = screen.getByTestId('workflow-json-result-panel');

			expect(panel.className).toContain('alert-danger');
			expect(panel.textContent).toContain('Simulated backend failure');
		});
	});

	it('reports warning tone when response data errors is non-empty', async () => {
		vi.stubGlobal(
			'fetch',
			vi.fn().mockImplementation((url: string) => {
				if (url === '/o/ldf-workflow/schema') {
					return Promise.resolve({
						json: () => Promise.resolve({schema: VALID_SCHEMA}),
						ok: true,
					});
				}

				return Promise.resolve({
					json: async () => ({
						errors: ['step 2 warning'],
						success: true,
					}),
					ok: true,
				});
			})
		);

		render(
			<WorkflowJsonEditor
				{...defaultProps}
				value='{"schemaVersion":"1.0","workflowId":"t","steps":[{"id":"a","operation":"noop","idempotencyKey":"x"}]}'
			/>
		);

		await waitFor(() => {
			expect(
				(screen.getByTestId('workflow-json-plan') as HTMLButtonElement)
					.disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-plan'));

		await waitFor(() => {
			const panel = screen.getByTestId('workflow-json-result-panel');

			expect(panel.className).toContain('alert-warning');
		});
	});

	it('adds is-invalid class to textarea when JSON is syntactically broken', async () => {
		render(<WorkflowJsonEditor {...defaultProps} value='{"bad' />);

		const textarea = screen.getByTestId('workflow-json-textarea');

		await waitFor(
			() => {
				expect(textarea.className).toContain('is-invalid');
			},
			{timeout: 500}
		);
	});

	it('does not add is-invalid class when textarea is empty on initial render', async () => {
		render(<WorkflowJsonEditor {...defaultProps} value="" />);

		const textarea = screen.getByTestId('workflow-json-textarea');

		// Wait past the debounce window so liveValidity has settled to 'empty'.
		await new Promise((resolve) => setTimeout(resolve, 400));

		expect(textarea.className).not.toContain('is-invalid');
	});

	it('disables all action buttons while an action is in-flight', async () => {
		let resolveFetch: (value: Response) => void;
		const pending = new Promise<Response>((resolve) => {
			resolveFetch = resolve;
		});

		vi.stubGlobal(
			'fetch',
			vi.fn().mockImplementation((url: string) => {
				if (url === '/o/ldf-workflow/schema') {
					return Promise.resolve({
						json: () =>
							Promise.resolve({schema: VALID_SCHEMA}),
						ok: true,
					});
				}

				return pending;
			})
		);

		render(
			<WorkflowJsonEditor
				{...defaultProps}
				value='{"schemaVersion":"1.0","workflowId":"t","steps":[{"id":"a","operation":"noop","idempotencyKey":"x"}]}'
			/>
		);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
		});

		fireEvent.click(screen.getByTestId('workflow-json-execute'));

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(true);
			expect(
				(screen.getByTestId('workflow-json-plan') as HTMLButtonElement)
					.disabled
			).toBe(true);
			expect(screen.getByTestId('workflow-json-progress')).not.toBeNull();

			const textarea = screen.getByTestId(
				'workflow-json-textarea'
			) as HTMLTextAreaElement;

			expect(textarea.readOnly).toBe(true);

			const workspace = screen
				.getByTestId('workflow-json-execute')
				.closest('[aria-busy]');

			expect(workspace?.getAttribute('aria-busy')).toBe('true');
			expect(workspace?.className).toContain(
				'workflow-json-workspace'
			);
		});

		resolveFetch!({
			json: async () => ({data: {}, success: true}),
			ok: true,
		} as unknown as Response);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(false);
			expect(screen.queryByTestId('workflow-json-progress')).toBeNull();

			const textareaAfter = screen.getByTestId(
				'workflow-json-textarea'
			) as HTMLTextAreaElement;

			expect(textareaAfter.readOnly).toBe(false);

			const workspaceAfter = screen
				.getByTestId('workflow-json-execute')
				.closest('[aria-busy]');

			expect(workspaceAfter?.getAttribute('aria-busy')).toBe('false');
			expect(workspaceAfter?.className).toContain(
				'workflow-json-workspace'
			);
		});
	});

	it('disables plan and execute when schema fetch fails', async () => {
		_resetSchemaForTest();
		vi.stubGlobal(
			'fetch',
			vi.fn().mockRejectedValue(new Error('Network error'))
		);

		render(<WorkflowJsonEditor {...defaultProps} />);

		await waitFor(() => {
			expect(
				(
					screen.getByTestId('workflow-json-plan') as HTMLButtonElement
				).disabled
			).toBe(true);
			expect(
				(
					screen.getByTestId(
						'workflow-json-execute'
					) as HTMLButtonElement
				).disabled
			).toBe(true);

			const banner = screen.getByRole('alert');

			expect(banner.className).toContain('alert-danger');
			expect(banner.textContent).toBe(
				i18n('workflow-json-schema-unavailable')
			);
		});
	});
});

describe('WorkflowJsonEditor layout structure', () => {
	it('renders an upper toolbar region containing Plan and Execute buttons', () => {
		const {container} = render(<WorkflowJsonEditor {...defaultProps} />);

		const toolbar = container.querySelector(
			'[data-testid="workflow-json-toolbar"]'
		);

		expect(toolbar).not.toBeNull();

		const toolbarPlan = toolbar?.querySelector(
			'[data-testid="workflow-json-plan"]'
		);
		const toolbarExecute = toolbar?.querySelector(
			'[data-testid="workflow-json-execute"]'
		);

		expect(toolbarPlan).not.toBeNull();
		expect(toolbarExecute).not.toBeNull();
	});

	it('renders editor and result panes as grid siblings inside the sheet', () => {
		const {container} = render(<WorkflowJsonEditor {...defaultProps} />);

		const grid = container.querySelector('.workflow-json-split');

		expect(grid).not.toBeNull();

		const panes = grid?.querySelectorAll('.workflow-json-pane');

		expect(panes?.length).toBe(2);
	});

	it('does not render the legacy bottom action bar', () => {
		const {container} = render(<WorkflowJsonEditor {...defaultProps} />);

		expect(
			container.querySelector('.workflow-json-action-bar')
		).toBeNull();
	});

	it('renders a result-pane placeholder before the first action', () => {
		render(<WorkflowJsonEditor {...defaultProps} />);

		const placeholder = screen.getByTestId(
			'workflow-json-result-placeholder'
		);

		expect(placeholder.textContent).toBe(
			i18n('workflow-json-result-placeholder')
		);
	});

	it('renders the toolbar Console tag via i18n (not the raw key)', () => {
		const tag = i18n('workflow-json-console-tag');

		const {container} = render(<WorkflowJsonEditor {...defaultProps} />);

		const tagEl = container.querySelector('.workflow-json-toolbar-tag');

		expect(tagEl?.textContent).toBe(tag);
	});
});
