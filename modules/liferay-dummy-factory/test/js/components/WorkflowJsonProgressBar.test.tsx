import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import WorkflowJsonProgressBar from '../../../src/main/resources/META-INF/resources/js/components/WorkflowJsonProgressBar';

describe('WorkflowJsonProgressBar', () => {
	it('renders nothing when busy is false', () => {
		const {container} = render(<WorkflowJsonProgressBar busy={false} />);

		expect(container.firstChild).toBeNull();
	});

	it('renders the progress bar when busy is true', () => {
		render(<WorkflowJsonProgressBar busy />);

		expect(screen.getByTestId('workflow-json-progress')).not.toBeNull();
	});

	it('sets aria-hidden to true so AT does not double-announce', () => {
		render(<WorkflowJsonProgressBar busy />);

		const node = screen.getByTestId('workflow-json-progress');

		expect(node.getAttribute('aria-hidden')).toBe('true');
	});

	it('uses Clay indeterminate progress classes', () => {
		render(<WorkflowJsonProgressBar busy />);

		const node = screen.getByTestId('workflow-json-progress');

		expect(node.className).toContain('progress');
		expect(node.className).toContain('progress-indeterminate');
	});
});
