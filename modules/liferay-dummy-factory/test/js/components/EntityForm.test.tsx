import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {MockedFunction} from 'vitest';

import EntityForm from '../../../src/main/resources/META-INF/resources/js/components/EntityForm';
import {useFormState} from '../../../src/main/resources/META-INF/resources/js/hooks/useFormState';
import {useProgress} from '../../../src/main/resources/META-INF/resources/js/hooks/useProgress';
import {EntityFormConfig} from '../../../src/main/resources/META-INF/resources/js/types';

vi.mock('../../../src/main/resources/META-INF/resources/js/hooks/useFormState');
vi.mock('../../../src/main/resources/META-INF/resources/js/hooks/useProgress');

const mockedUseFormState = useFormState as MockedFunction<typeof useFormState>;
const mockedUseProgress = useProgress as MockedFunction<typeof useProgress>;

const config: EntityFormConfig = {
	actionURL: '/ldf/user',
	entityType: 'USER',
	fields: [],
	helpText: 'help',
	icon: 'user',
	label: 'user',
};

function mockFormStateSubmitting(submitting: boolean) {
	mockedUseFormState.mockReturnValue({
		submitting,
		validate: vi.fn().mockReturnValue(true),
	} as unknown as ReturnType<typeof useFormState>);
}

describe('EntityForm i18n submit button', () => {
	beforeEach(() => {
		mockedUseFormState.mockReset();
		mockedUseProgress.mockReset();

		mockedUseProgress.mockReturnValue({
			running: false,
		} as unknown as ReturnType<typeof useProgress>);
	});

	it("renders the Run label via Liferay.Language.get('run') when idle", () => {
		mockFormStateSubmitting(false);

		render(
			<EntityForm
				actionResourceURLs={{'/ldf/user': '/o/user'}}
				config={config}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		expect(screen.queryByText(Liferay.Language.get('run'))).not.toBeNull();
		expect(screen.queryByText(Liferay.Language.get('running'))).toBeNull();
	});

	it("renders the Running label via Liferay.Language.get('running') when submitting", () => {
		mockFormStateSubmitting(true);

		render(
			<EntityForm
				actionResourceURLs={{'/ldf/user': '/o/user'}}
				config={config}
				dataResourceURL="/o/data"
				progressResourceURL="/o/progress"
			/>
		);

		expect(
			screen.queryByText(Liferay.Language.get('running'))
		).not.toBeNull();
		expect(screen.queryByText(Liferay.Language.get('run'))).toBeNull();
	});
});
