import {useState} from 'react';

import {
	EntityFormConfig,
	FIELD_GROUPS,
	FieldDefinition,
	MultiSiteResult,
} from '../types';
import {useFormState} from '../hooks/useFormState';
import {useProgress} from '../hooks/useProgress';
import {postResource} from '../utils/api';
import {translate} from '../utils/i18n';
import DynamicSelect from './DynamicSelect';
import FormField from './FormField';
import ProgressBar from './ProgressBar';
import ResultAlert from './ResultAlert';

interface EntityFormProps {
	actionResourceURLs: Record<string, string>;
	config: EntityFormConfig;
	dataResourceURL: string;
	progressResourceURL: string;
}

function EntityForm({
	actionResourceURLs,
	config,
	dataResourceURL,
	progressResourceURL,
}: EntityFormProps) {
	const {
		endSubmit,
		errors,
		setValue,
		startSubmit,
		submitting,
		validate,
		values,
	} = useFormState(config.fields);
	const [result, setResult] = useState<{
		message: string;
		multiSite?: MultiSiteResult | null;
		type: 'success' | 'warning' | 'danger';
	} | null>(null);
	const {percent, reset, running, start} = useProgress(progressResourceURL);

	const isFieldVisible = (field: FieldDefinition): boolean => {
		if (!field.visibleWhen) {
			return true;
		}

		const controlValue = values[field.visibleWhen.field] || '';
		const allowedValues = Array.isArray(field.visibleWhen.value)
			? field.visibleWhen.value
			: [field.visibleWhen.value];

		return allowedValues.includes(String(controlValue));
	};

	const requiredFields = config.fields.filter((f) => !f.advanced);
	const advancedFields = config.fields.filter((f) => f.advanced);
	const visibleAdvanced = advancedFields.filter(isFieldVisible);
	const uploadURL = actionResourceURLs['/ldf/doc/upload'] ?? '';

	const entityKey = config.entityType.toLowerCase().replace(/_/g, '-');

	const toKebab = (value: string) =>
		value.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase();

	const getFieldTestId = (field: FieldDefinition) => {
		const fieldKey = toKebab(field.name);
		const suffixByType: Record<string, string> = {
			file: 'file',
			multiselect: 'select',
			number: 'input',
			select: 'select',
			text: 'input',
			textarea: 'textarea',
			toggle: 'toggle',
		};
		const suffix = suffixByType[field.type] ?? 'input';

		return `${entityKey}-${fieldKey}-${suffix}`;
	};

	const handleSubmit = async () => {
		const visibleFields = config.fields.filter(isFieldVisible);

		if (!validate(visibleFields)) {
			return;
		}

		startSubmit();
		setResult(null);
		start('COMMON_PROGRESS_ID');

		const actionURL = actionResourceURLs[config.actionURL];

		if (!actionURL) {
			setResult({
				message: `Missing resource URL for ${config.actionURL}`,
				type: 'danger',
			});
			endSubmit();
			reset();
			return;
		}

		const submitValues: Record<
			string,
			string | number | boolean | number[]
		> = {...values};

		for (const field of config.fields) {
			if (field.type === 'multiselect' && submitValues[field.name]) {
				submitValues[field.name] = String(submitValues[field.name])
					.split(',')
					.filter(Boolean)
					.map(Number);
			}
		}

		const response = await postResource(actionURL, submitValues);

		endSubmit();
		reset();

		const payload = response.success
			? (response.data as unknown as Partial<MultiSiteResult> | undefined)
			: undefined;

		if (payload && Array.isArray(payload.perSite)) {
			const multiSite = payload as MultiSiteResult;

			if (multiSite.ok) {
				setResult({
					message: translate('execution-completed-successfully'),
					multiSite,
					type: 'success',
				});
			}
			else if (multiSite.totalCreated > 0) {
				setResult({
					message: translate('partial-execution'),
					multiSite,
					type: 'warning',
				});
			}
			else {
				setResult({
					message: translate('execution-failed'),
					multiSite,
					type: 'danger',
				});
			}
		}
		else if (response.success) {
			setResult({
				message: translate('execution-completed-successfully'),
				type: 'success',
			});
		}
		else {
			setResult({
				message: response.error ?? translate('execution-failed'),
				type: 'danger',
			});
		}
	};

	const renderField = (field: FieldDefinition) => {
		if (field.dataSource) {
			const dependsOnValue = field.dependsOn
				? String(values[field.dependsOn.field] || '')
				: undefined;

			return (
				<DynamicSelect
					dataResourceURL={dataResourceURL}
					dependsOnValue={dependsOnValue}
					error={errors[field.name]}
					field={field}
					key={field.name + (dependsOnValue || '')}
					onChange={setValue}
					testId={getFieldTestId(field)}
					value={values[field.name] || ''}
				/>
			);
		}

		return (
			<FormField
				error={errors[field.name]}
				field={field}
				formValues={values}
				key={field.name}
				onChange={setValue}
				testId={getFieldTestId(field)}
				uploadURL={uploadURL}
				value={values[field.name] || ''}
			/>
		);
	};

	return (
		<div className="sheet">
			<div className="sheet-header">
				<h2>{translate(config.label)}</h2>

				{config.helpText && (
					<p className="sheet-text">{translate(config.helpText)}</p>
				)}
			</div>

			<div className="sheet-section">
				{requiredFields.filter(isFieldVisible).map(renderField)}

				{visibleAdvanced.filter((f) => !f.group).map(renderField)}

				{FIELD_GROUPS.map((group) => {
					const groupFields = visibleAdvanced.filter(
						(f) => f.group === group
					);

					if (groupFields.length === 0) {
						return null;
					}

					return (
						<div key={group}>
							<h5>{translate(`section.${group}`)}</h5>

							<hr />

							{groupFields.map(renderField)}
						</div>
					);
				})}
			</div>

			<ProgressBar
				percent={percent}
				running={running}
				testId={`${entityKey}-progress`}
			/>

			<div className="sheet-footer">
				<button
					className="btn btn-primary"
					data-testid={`${entityKey}-submit`}
					disabled={submitting}
					onClick={handleSubmit}
					type="button"
				>
					{submitting ? translate('running') : translate('run')}
				</button>
			</div>

			{result && (
				<ResultAlert
					message={result.message}
					multiSite={result.multiSite}
					onDismiss={() => setResult(null)}
					testId={`${entityKey}-result`}
					type={result.type}
				/>
			)}
		</div>
	);
}

export default EntityForm;
