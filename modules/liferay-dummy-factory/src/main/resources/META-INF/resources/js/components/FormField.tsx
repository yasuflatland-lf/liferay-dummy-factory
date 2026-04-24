import {FieldDefinition, SelectOption} from '../types';
import {translate} from '../utils/i18n';
import FileUploadArea from './FileUploadArea';

interface FormFieldProps {
	disabled?: boolean;
	error?: string;
	field: FieldDefinition;
	formValues?: Record<string, string>;
	onChange: (name: string, value: string) => void;
	options?: SelectOption[];
	testId?: string;
	uploadURL?: string;
	value: string;
}

function FieldLabel({field}: {field: FieldDefinition}) {
	return (
		<label htmlFor={field.name}>
			{translate(field.label)}

			{field.required && (
				<span className="reference-mark text-warning">*</span>
			)}
		</label>
	);
}

function FieldError({error}: {error?: string}) {
	if (!error) {
		return null;
	}

	return <div className="form-feedback-item">{error}</div>;
}

function FormField({
	disabled = false,
	error,
	field,
	formValues,
	onChange,
	options,
	testId,
	uploadURL,
	value,
}: FormFieldProps) {
	const resolvedOptions = options || field.options || [];

	if (field.type === 'toggle') {
		return (
			<div className="form-group">
				<label className="toggle-switch" htmlFor={field.name}>
					<input
						aria-disabled={disabled || undefined}
						checked={value === 'true'}
						className="toggle-switch-check"
						data-testid={testId}
						disabled={disabled}
						id={field.name}
						onChange={(e) =>
							onChange(field.name, String(e.target.checked))
						}
						type="checkbox"
					/>

					<span aria-hidden="true" className="toggle-switch-bar">
						<span className="toggle-switch-handle" />
					</span>

					<span className="toggle-switch-text">
						{translate(field.label)}
					</span>
				</label>
			</div>
		);
	}

	if (field.type === 'multiselect') {
		const selectedValues = value ? value.split(',').filter(Boolean) : [];

		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<FieldLabel field={field} />

				<select
					aria-disabled={disabled || undefined}
					className="form-control"
					data-testid={testId}
					disabled={disabled}
					id={field.name}
					multiple
					onChange={(e) => {
						const selected = Array.from(
							e.target.selectedOptions,
							(opt) => opt.value
						);

						onChange(field.name, selected.join(','));
					}}
					value={selectedValues}
				>
					{resolvedOptions.map((opt) => (
						<option key={opt.value} value={opt.value}>
							{translate(opt.label)}
						</option>
					))}
				</select>

				<FieldError error={error} />
			</div>
		);
	}

	if (field.type === 'select') {
		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<FieldLabel field={field} />

				<select
					aria-disabled={disabled || undefined}
					className="form-control"
					data-testid={testId}
					disabled={disabled}
					id={field.name}
					onChange={(e) => onChange(field.name, e.target.value)}
					value={value}
				>
					<option value="">{translate('select')}</option>

					{resolvedOptions.map((opt) => (
						<option key={opt.value} value={opt.value}>
							{translate(opt.label)}
						</option>
					))}
				</select>

				<FieldError error={error} />
			</div>
		);
	}

	if (field.type === 'textarea') {
		return (
			<div className={`form-group ${error ? 'has-error' : ''}`}>
				<FieldLabel field={field} />

				<textarea
					aria-disabled={disabled || undefined}
					className="form-control"
					data-testid={testId}
					disabled={disabled}
					id={field.name}
					onChange={(e) => onChange(field.name, e.target.value)}
					rows={5}
					value={value}
				/>

				<FieldError error={error} />
			</div>
		);
	}

	if (field.type === 'file') {
		const groupId = String(formValues?.['groupId'] ?? '');

		return (
			<FileUploadArea
				groupId={groupId}
				key={field.name}
				label={field.label}
				onChange={(name, newValue) => onChange(field.name, newValue)}
				testId={testId}
				uploadURL={uploadURL ?? ''}
				value={value}
			/>
		);
	}

	return (
		<div className={`form-group ${error ? 'has-error' : ''}`}>
			<FieldLabel field={field} />

			<input
				aria-disabled={disabled || undefined}
				className="form-control"
				data-testid={testId}
				disabled={disabled}
				id={field.name}
				onChange={(e) => onChange(field.name, e.target.value)}
				type={field.type === 'number' ? 'number' : 'text'}
				value={value}
			/>

			<FieldError error={error} />
		</div>
	);
}

export default FormField;
