import {FieldDefinition} from '../types';
import {useApiData} from '../hooks/useApiData';
import {translate} from '../utils/i18n';

import FormField from './FormField';

interface DynamicSelectProps {
	dataResourceURL?: string;
	dependsOnValue?: string;
	disabled?: boolean;
	error?: string;
	field: FieldDefinition;
	onChange: (name: string, value: string) => void;
	testId?: string;
	value: string;
}

function DynamicSelect({
	dataResourceURL,
	dependsOnValue,
	disabled = false,
	error,
	field,
	onChange,
	testId,
	value,
}: DynamicSelectProps) {
	const extraParams = field.dependsOn
		? {[field.dependsOn.paramName]: dependsOnValue ?? ''}
		: undefined;

	const {
		data,
		error: apiError,
		loading,
	} = useApiData(dataResourceURL, field.dataSource, extraParams);

	if (field.dependsOn && !dependsOnValue) {
		return (
			<div className="form-group">
				<label htmlFor={field.name}>{translate(field.label)}</label>

				<select
					className="form-control"
					data-testid={testId}
					disabled
					id={field.name}
				>
					<option>{translate('please-select-parent-first')}</option>
				</select>
			</div>
		);
	}

	if (loading) {
		return (
			<div className="form-group">
				<label htmlFor={field.name}>{translate(field.label)}</label>

				<div className="loading-animation loading-animation-sm" />
			</div>
		);
	}

	if (apiError) {
		return (
			<div className="form-group">
				<label htmlFor={field.name}>{translate(field.label)}</label>

				<div className="alert alert-danger">{apiError}</div>
			</div>
		);
	}

	return (
		<FormField
			disabled={disabled}
			error={error}
			field={field}
			onChange={onChange}
			options={data}
			testId={testId}
			value={value}
		/>
	);
}

export default DynamicSelect;
