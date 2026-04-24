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

describe('FormField disabledWhen', () => {
	it('passes disabled to the underlying text input', () => {
		const field: FieldDefinition = {
			label: 'name',
			name: 'name',
			required: false,
			type: 'text',
		};

		render(<FormField disabled={true} field={field} onChange={noop} value="" />);

		const input = screen.getByRole('textbox') as HTMLInputElement;

		expect(input.disabled).toBe(true);
		expect(input.getAttribute('aria-disabled')).toBe('true');
	});

	it('passes disabled to the underlying toggle input', () => {
		const field: FieldDefinition = {
			label: 'name',
			name: 'name',
			required: false,
			type: 'toggle',
		};

		render(
			<FormField disabled={true} field={field} onChange={noop} value="false" />
		);

		const checkbox = screen.getByRole('checkbox') as HTMLInputElement;

		expect(checkbox.disabled).toBe(true);
		expect(checkbox.getAttribute('aria-disabled')).toBe('true');
	});

	it('passes disabled to the underlying select input', () => {
		const field: FieldDefinition = {
			label: 'role',
			name: 'role',
			options: [{label: 'administrator', value: 'admin'}],
			required: false,
			type: 'select',
		};

		render(<FormField disabled={true} field={field} onChange={noop} value="" />);

		const select = screen.getByRole('combobox') as HTMLSelectElement;

		expect(select.disabled).toBe(true);
		expect(select.getAttribute('aria-disabled')).toBe('true');
	});

	it('passes disabled to the underlying multiselect input', () => {
		const field: FieldDefinition = {
			label: 'roles',
			name: 'roles',
			options: [{label: 'administrator', value: 'admin'}],
			required: false,
			type: 'multiselect',
		};

		render(<FormField disabled={true} field={field} onChange={noop} value="" />);

		const select = screen.getByRole('listbox') as HTMLSelectElement;

		expect(select.disabled).toBe(true);
		expect(select.getAttribute('aria-disabled')).toBe('true');
	});

	it('passes disabled to the underlying textarea input', () => {
		const field: FieldDefinition = {
			label: 'description',
			name: 'description',
			required: false,
			type: 'textarea',
		};

		render(<FormField disabled={true} field={field} onChange={noop} value="" />);

		const textarea = screen.getByRole('textbox') as HTMLTextAreaElement;

		expect(textarea.disabled).toBe(true);
		expect(textarea.getAttribute('aria-disabled')).toBe('true');
	});

	it('does not set disabled when prop is omitted (default)', () => {
		const field: FieldDefinition = {
			label: 'name',
			name: 'name',
			required: false,
			type: 'text',
		};

		render(<FormField field={field} onChange={noop} value="" />);

		const input = screen.getByRole('textbox') as HTMLInputElement;

		expect(input.disabled).toBe(false);
		expect(input.getAttribute('aria-disabled')).toBeNull();
	});
});

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
