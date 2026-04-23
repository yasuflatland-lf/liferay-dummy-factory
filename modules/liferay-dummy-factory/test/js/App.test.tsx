import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import App from '../../src/main/resources/META-INF/resources/js/App';

vi.mock(
	'../../src/main/resources/META-INF/resources/js/components/EntityForm',
	() => ({
		default: () => <div data-testid="entity-form" />,
	})
);

vi.mock(
	'../../src/main/resources/META-INF/resources/js/components/WorkflowJsonEditor',
	() => ({
		default: () => <div data-testid="workflow-json-editor" />,
	})
);

describe('App tabs', () => {
	it('shows create entities by default and excludes workflow json from the selector', () => {
		render(
			<App
				actionResourceURLs={{}}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		expect(screen.getByTestId('app-tab-create-entities')).not.toBeNull();
		expect(screen.getByTestId('entity-selector')).not.toBeNull();
		expect(screen.getByTestId('entity-selector-USERS')).not.toBeNull();
		expect(
			screen.queryByTestId('entity-selector-WORKFLOW_JSON')
		).toBeNull();
		expect(screen.getByTestId('entity-form')).not.toBeNull();
		expect(screen.queryByTestId('workflow-json-editor')).toBeNull();
	});

	it('switches to the workflow json tab without rendering the entity selector', () => {
		render(
			<App
				actionResourceURLs={{}}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		fireEvent.click(screen.getByTestId('app-tab-workflow-json'));

		expect(screen.getByTestId('workflow-json-editor')).not.toBeNull();
		expect(screen.queryByTestId('entity-selector')).toBeNull();
		expect(screen.queryByTestId('entity-form')).toBeNull();
	});
});
