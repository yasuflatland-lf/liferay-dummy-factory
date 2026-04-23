import {describe, expect, it} from 'vitest';

import {
	validateField,
	validateForm,
} from '../../../src/main/resources/META-INF/resources/js/utils/validation';
import {FieldDefinition} from '../../../src/main/resources/META-INF/resources/js/types';

function makeField(overrides: Partial<FieldDefinition> = {}): FieldDefinition {
	return {
		label: 'Test Field',
		name: 'testField',
		required: false,
		type: 'text',
		...overrides,
	};
}

describe('validateField', () => {
	describe('required check (field.required flag)', () => {
		it('returns resolved i18n message for empty required field', () => {
			const field = makeField({required: true});

			const result = validateField('', field);

			expect(result).not.toBe('this-field-is-required');
			expect(result).toBe('This Field Is Required');
		});

		it('returns resolved i18n message for whitespace-only required field', () => {
			const field = makeField({required: true});

			const result = validateField('   ', field);

			expect(result).not.toBe('this-field-is-required');
			expect(result).toBe('This Field Is Required');
		});

		it('returns null for non-empty required field', () => {
			const field = makeField({required: true});

			const result = validateField('hello', field);

			expect(result).toBeNull();
		});

		it('returns null for empty non-required field', () => {
			const field = makeField({required: false});

			const result = validateField('', field);

			expect(result).toBeNull();
		});
	});

	describe('digits validator (via runValidator)', () => {
		const field = makeField({
			validators: [
				{
					message: 'please-enter-a-valid-number',
					type: 'digits',
				},
			],
		});

		it('returns resolved i18n message for non-digit input', () => {
			const result = validateField('abc', field);

			expect(result).not.toBe('please-enter-a-valid-number');
			expect(result).toBe('Please Enter a Valid Number');
		});

		it('returns resolved i18n message for input with decimal point', () => {
			const result = validateField('3.14', field);

			expect(result).not.toBe('please-enter-a-valid-number');
			expect(result).toBe('Please Enter a Valid Number');
		});

		it('returns resolved i18n message for input with spaces', () => {
			const result = validateField('1 2 3', field);

			expect(result).not.toBe('please-enter-a-valid-number');
			expect(result).toBe('Please Enter a Valid Number');
		});

		it('returns null for valid digit-only input', () => {
			const result = validateField('123', field);

			expect(result).toBeNull();
		});

		it('returns null for empty input (digits validator skips empty)', () => {
			const result = validateField('', field);

			expect(result).toBeNull();
		});
	});

	describe('min validator (via runValidator)', () => {
		const field = makeField({
			validators: [
				{
					message: 'value-must-be-greater-than-0',
					type: 'min',
					value: 1,
				},
			],
		});

		it('returns resolved i18n message when value is below minimum', () => {
			const result = validateField('0', field);

			expect(result).not.toBe('value-must-be-greater-than-0');
			expect(result).toBe('Value Must Be Greater Than 0');
		});

		it('returns resolved i18n message for negative value', () => {
			const result = validateField('-5', field);

			expect(result).not.toBe('value-must-be-greater-than-0');
			expect(result).toBe('Value Must Be Greater Than 0');
		});

		it('returns null when value equals minimum', () => {
			const result = validateField('1', field);

			expect(result).toBeNull();
		});

		it('returns null when value exceeds minimum', () => {
			const result = validateField('10', field);

			expect(result).toBeNull();
		});

		it('returns null when validator.value is undefined', () => {
			const fieldNoValue = makeField({
				validators: [
					{
						message: 'value-must-be-greater-than-0',
						type: 'min',
					},
				],
			});

			const result = validateField('0', fieldNoValue);

			expect(result).toBeNull();
		});
	});

	describe('max validator (via runValidator)', () => {
		const field = makeField({
			validators: [
				{
					message: 'please-enter-a-valid-number',
					type: 'max',
					value: 100,
				},
			],
		});

		it('returns resolved i18n message when value exceeds maximum', () => {
			const result = validateField('101', field);

			expect(result).not.toBe('please-enter-a-valid-number');
			expect(result).toBe('Please Enter a Valid Number');
		});

		it('returns null when value equals maximum', () => {
			const result = validateField('100', field);

			expect(result).toBeNull();
		});

		it('returns null when value is below maximum', () => {
			const result = validateField('50', field);

			expect(result).toBeNull();
		});

		it('returns null when validator.value is undefined', () => {
			const fieldNoValue = makeField({
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'max',
					},
				],
			});

			const result = validateField('999', fieldNoValue);

			expect(result).toBeNull();
		});
	});

	describe('required validator type (via runValidator)', () => {
		const field = makeField({
			validators: [
				{
					message: 'this-field-is-required',
					type: 'required',
				},
			],
		});

		it('returns resolved i18n message for empty string', () => {
			const result = validateField('', field);

			expect(result).not.toBe('this-field-is-required');
			expect(result).toBe('This Field Is Required');
		});

		it('returns resolved i18n message for whitespace-only string', () => {
			const result = validateField('   ', field);

			expect(result).not.toBe('this-field-is-required');
			expect(result).toBe('This Field Is Required');
		});

		it('returns null for non-empty string', () => {
			const result = validateField('value', field);

			expect(result).toBeNull();
		});
	});

	describe('multiple validators', () => {
		it('returns first failing validator message', () => {
			const field = makeField({
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
					{
						message: 'value-must-be-greater-than-0',
						type: 'min',
						value: 1,
					},
				],
			});

			const result = validateField('abc', field);

			expect(result).toBe('Please Enter a Valid Number');
		});

		it('checks second validator when first passes', () => {
			const field = makeField({
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
					{
						message: 'value-must-be-greater-than-0',
						type: 'min',
						value: 1,
					},
				],
			});

			const result = validateField('0', field);

			expect(result).toBe('Value Must Be Greater Than 0');
		});

		it('returns null when all validators pass', () => {
			const field = makeField({
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
					{
						message: 'value-must-be-greater-than-0',
						type: 'min',
						value: 1,
					},
				],
			});

			const result = validateField('5', field);

			expect(result).toBeNull();
		});
	});

	describe('field.required takes precedence over validators', () => {
		it('returns required message before running validators on empty input', () => {
			const field = makeField({
				required: true,
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
				],
			});

			const result = validateField('', field);

			expect(result).toBe('This Field Is Required');
		});
	});

	describe('field with no validators', () => {
		it('returns null for non-required field without validators', () => {
			const field = makeField();

			const result = validateField('anything', field);

			expect(result).toBeNull();
		});
	});
});

describe('validateForm', () => {
	it('returns errors for invalid fields', () => {
		const fields: FieldDefinition[] = [
			makeField({
				name: 'count',
				required: true,
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
				],
			}),
			makeField({name: 'baseName', required: true}),
		];

		const errors = validateForm({baseName: '', count: 'abc'}, fields);

		expect(errors.count).toBe('Please Enter a Valid Number');
		expect(errors.baseName).toBe('This Field Is Required');
	});

	it('returns empty object when all fields are valid', () => {
		const fields: FieldDefinition[] = [
			makeField({
				name: 'count',
				validators: [
					{
						message: 'please-enter-a-valid-number',
						type: 'digits',
					},
				],
			}),
			makeField({name: 'baseName'}),
		];

		const errors = validateForm({baseName: 'Test', count: '5'}, fields);

		expect(errors).toEqual({});
	});

	it('uses empty string for missing form values', () => {
		const fields: FieldDefinition[] = [
			makeField({name: 'requiredField', required: true}),
		];

		const errors = validateForm({}, fields);

		expect(errors.requiredField).toBe('This Field Is Required');
	});
});
