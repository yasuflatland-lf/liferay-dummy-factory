import {FieldDefinition, Validator} from '../types';
import {translate} from './i18n';

export function validateField(
	value: string,
	field: FieldDefinition
): string | null {
	if (field.required && !value.trim()) {
		return translate('this-field-is-required');
	}

	if (field.validators) {
		for (const validator of field.validators) {
			const error = runValidator(value, validator);

			if (error) {
				return error;
			}
		}
	}

	return null;
}

function runValidator(value: string, validator: Validator): string | null {
	switch (validator.type) {
		case 'digits':
			if (value && !/^\d+$/.test(value)) {
				return translate(validator.message);
			}
			break;
		case 'min':
			if (
				validator.value !== undefined &&
				Number(value) < validator.value
			) {
				return translate(validator.message);
			}
			break;
		case 'max':
			if (
				validator.value !== undefined &&
				Number(value) > validator.value
			) {
				return translate(validator.message);
			}
			break;
		case 'required':
			if (!value.trim()) {
				return translate(validator.message);
			}
			break;
	}

	return null;
}

export function validateForm(
	values: Record<string, string>,
	fields: FieldDefinition[]
): Record<string, string> {
	const errors: Record<string, string> = {};

	for (const field of fields) {
		const error = validateField(values[field.name] || '', field);

		if (error) {
			errors[field.name] = error;
		}
	}

	return errors;
}
