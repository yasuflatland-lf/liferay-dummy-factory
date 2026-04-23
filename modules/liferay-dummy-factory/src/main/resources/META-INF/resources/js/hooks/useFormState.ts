import {useCallback, useReducer} from 'react';

import {FieldDefinition} from '../types';
import {validateForm} from '../utils/validation';

interface FormState {
	errors: Record<string, string>;
	submitting: boolean;
	values: Record<string, string>;
}

type FormAction =
	| {field: string; type: 'SET_VALUE'; value: string}
	| {errors: Record<string, string>; type: 'SET_ERRORS'}
	| {type: 'START_SUBMIT'}
	| {type: 'END_SUBMIT'};

function formReducer(state: FormState, action: FormAction): FormState {
	switch (action.type) {
		case 'SET_VALUE':
			return {
				...state,
				errors: {...state.errors, [action.field]: ''},
				values: {...state.values, [action.field]: action.value},
			};
		case 'SET_ERRORS':
			return {...state, errors: action.errors};
		case 'START_SUBMIT':
			return {...state, submitting: true};
		case 'END_SUBMIT':
			return {...state, submitting: false};
		default:
			return state;
	}
}

export function useFormState(fields: FieldDefinition[]) {
	const initialValues: Record<string, string> = {};

	for (const field of fields) {
		if (field.defaultValue !== undefined) {
			initialValues[field.name] = String(field.defaultValue);
		}
	}

	const [state, dispatch] = useReducer(formReducer, {
		errors: {},
		submitting: false,
		values: initialValues,
	});

	const setValue = useCallback((field: string, value: string) => {
		dispatch({field, type: 'SET_VALUE', value});
	}, []);

	const validate = useCallback(
		(visibleFields?: FieldDefinition[]): boolean => {
			const fieldsToValidate = visibleFields || fields;
			const errors = validateForm(state.values, fieldsToValidate);

			dispatch({errors, type: 'SET_ERRORS'});

			return Object.keys(errors).length === 0;
		},
		[fields, state.values]
	);

	const startSubmit = useCallback(() => {
		dispatch({type: 'START_SUBMIT'});
	}, []);

	const endSubmit = useCallback(() => {
		dispatch({type: 'END_SUBMIT'});
	}, []);

	return {
		...state,
		endSubmit,
		setValue,
		startSubmit,
		validate,
	};
}
