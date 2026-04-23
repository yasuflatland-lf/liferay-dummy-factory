import {useState} from 'react';

import {translate} from '../utils/i18n';

interface FileUploadAreaProps {
	groupId: string;
	label: string;
	onChange: (name: string, value: string) => void;
	testId?: string;
	uploadURL: string;
	value: string;
}

const FIELD_NAME = 'uploadedFiles';

function FileUploadArea({
	groupId,
	label,
	onChange,
	testId,
	uploadURL,
	value,
}: FileUploadAreaProps) {
	const [uploadingNames, setUploadingNames] = useState<string[]>([]);
	const [errors, setErrors] = useState<Record<string, string>>({});

	const fileNames = value ? value.split(',').filter(Boolean) : [];

	const uploadFile = async (file: File) => {
		const formData = new FormData();

		formData.append('cmd', 'add_temp');
		formData.append('groupId', groupId);
		formData.append('file', file);

		try {
			const response = await fetch(uploadURL, {
				body: formData,
				credentials: 'include',
				headers: {
					'x-csrf-token': Liferay.authToken,
				},
				method: 'POST',
			});

			if (!response.ok) {
				throw new Error(`Server error: ${response.status}`);
			}

			const data = await response.json();

			if (!data.success || !data.fileName) {
				throw new Error(data.error || translate('upload-failed'));
			}

			return {fileName: data.fileName as string, sourceName: file.name};
		}
		catch (error) {
			const message =
				error instanceof Error
					? error.message
					: translate('upload-failed');

			console.error('File upload failed', error);

			setErrors((prev) => ({...prev, [file.name]: message}));

			return null;
		}
	};

	const handleFileChange = async (target: HTMLInputElement) => {
		const fileList = target.files;

		if (!fileList || fileList.length === 0) {
			return;
		}

		const files = Array.from(fileList);
		const sourceNames = files.map((file) => file.name);

		setUploadingNames((prev) => [...prev, ...sourceNames]);
		setErrors((prev) => {
			const next = {...prev};

			for (const name of sourceNames) {
				delete next[name];
			}

			return next;
		});

		let accumulated = fileNames.slice();

		for (const file of files) {
			const result = await uploadFile(file);

			if (result) {
				accumulated = [...accumulated, result.fileName];
				onChange(FIELD_NAME, accumulated.join(','));
			}
		}

		setUploadingNames((prev) =>
			prev.filter((name) => !sourceNames.includes(name))
		);

		target.value = '';
	};

	const handleRemove = async (fileName: string) => {
		try {
			const formData = new FormData();

			formData.append('cmd', 'delete_temp');
			formData.append('groupId', groupId);
			formData.append('fileName', fileName);

			const response = await fetch(uploadURL, {
				body: formData,
				credentials: 'include',
				headers: {
					'x-csrf-token': Liferay.authToken,
				},
				method: 'POST',
			});

			const data = await response.json();

			if (response.ok && data.success) {
				const next = fileNames.filter((name) => name !== fileName);

				onChange(FIELD_NAME, next.join(','));
			}
			else {
				setErrors((prev) => ({
					...prev,
					[fileName]: data.error || translate('upload-failed'),
				}));
			}
		}
		catch (error) {
			console.error(`Failed to delete temp file ${fileName}`, error);

			setErrors((prev) => ({
				...prev,
				[fileName]: translate('upload-failed'),
			}));
		}
	};

	const errorEntries = Object.entries(errors);

	return (
		<div className="form-group">
			<label htmlFor="ldf-file-upload-area">{translate(label)}</label>

			<input
				className="form-control"
				data-testid={testId ? `${testId}-input` : undefined}
				id="ldf-file-upload-area"
				multiple
				onChange={(e) => handleFileChange(e.target)}
				type="file"
			/>

			{uploadingNames.length > 0 && (
				<div className="form-text">
					{translate('uploading')}
					{': '}
					{uploadingNames.join(', ')}
				</div>
			)}

			{fileNames.length > 0 && (
				<ul
					className="list-inline mt-2"
					data-testid={testId ? `${testId}-uploaded-list` : undefined}
				>
					{fileNames.map((fileName, index) => (
						<li className="list-inline-item" key={fileName}>
							<span className="label label-secondary">
								<span className="label-item label-item-expand">
									{fileName}
								</span>

								<span className="label-item label-item-after">
									<button
										aria-label={translate('remove')}
										className="btn btn-unstyled"
										data-testid={
											testId
												? `${testId}-remove-${index}`
												: undefined
										}
										onClick={() => handleRemove(fileName)}
										type="button"
									>
										<span aria-hidden="true">&times;</span>
									</button>
								</span>
							</span>
						</li>
					))}
				</ul>
			)}

			{errorEntries.length > 0 && (
				<ul className="list-unstyled mt-2">
					{errorEntries.map(([name, message]) => (
						<li className="text-danger" key={name}>
							{name}
							{': '}
							{message}
						</li>
					))}
				</ul>
			)}
		</div>
	);
}

export default FileUploadArea;
