import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ResultAlert from '../../../src/main/resources/META-INF/resources/js/components/ResultAlert';

function i18n(key: string): string {
	const text = Liferay.Language.get(key);

	expect(text).not.toBe(key);
	expect(text.length).toBeGreaterThan(0);

	return text;
}

const noop = () => undefined;

describe('ResultAlert i18n', () => {
	it('renders the success i18n message when type is success', () => {
		const successText = i18n('execution-completed-successfully');

		render(
			<ResultAlert
				message={successText}
				onDismiss={noop}
				type="success"
			/>
		);

		expect(screen.queryByText(successText)).not.toBeNull();
	});

	it('renders the failed i18n message when type is danger', () => {
		const failedText = i18n('execution-failed');

		render(
			<ResultAlert message={failedText} onDismiss={noop} type="danger" />
		);

		expect(screen.queryByText(failedText)).not.toBeNull();
	});

	it('renders the partial execution i18n message when type is warning', () => {
		const partialText = i18n('partial-execution');

		render(
			<ResultAlert
				message={partialText}
				onDismiss={noop}
				type="warning"
			/>
		);

		expect(screen.queryByText(partialText)).not.toBeNull();
	});
});
