import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import FormField from '../../../src/main/resources/META-INF/resources/js/components/FormField';
import {FieldDefinition} from '../../../src/main/resources/META-INF/resources/js/types';

function i18n(key: string): string {
	const text = Liferay.Language.get(key);

	expect(text).not.toBe(key);
	expect(text.length).toBeGreaterThan(0);

	return text;
}

const noop = () => undefined;

const textField: FieldDefinition = {
	label: 'name',
	name: 'name',
	required: true,
	type: 'text',
};

const selectField: FieldDefinition = {
	label: 'role',
	name: 'role',
	options: [
		{label: 'administrator', value: 'admin'},
		{label: 'user', value: 'user'},
	],
	required: true,
	type: 'select',
};

describe('FormField i18n', () => {
	it('renders the required validation i18n message when error is set on a text field', () => {
		const requiredText = i18n('this-field-is-required');

		render(
			<FormField
				error={requiredText}
				field={textField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(requiredText)).not.toBeNull();
	});

	it('renders the required validation i18n message when error is set on a textarea field', () => {
		const requiredText = i18n('this-field-is-required');

		const textareaField: FieldDefinition = {
			label: 'description',
			name: 'description',
			required: true,
			type: 'textarea',
		};

		render(
			<FormField
				error={requiredText}
				field={textareaField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(requiredText)).not.toBeNull();
	});

	it('renders the select placeholder i18n message for select fields', () => {
		const selectText = i18n('select');

		render(<FormField field={selectField} onChange={noop} value="" />);

		expect(screen.queryByText(selectText)).not.toBeNull();
	});

	it('renders the required validation i18n message alongside the select placeholder when error is set', () => {
		const selectText = i18n('select');
		const requiredText = i18n('this-field-is-required');

		render(
			<FormField
				error={requiredText}
				field={selectField}
				onChange={noop}
				value=""
			/>
		);

		expect(screen.queryByText(selectText)).not.toBeNull();
		expect(screen.queryByText(requiredText)).not.toBeNull();
	});
});
