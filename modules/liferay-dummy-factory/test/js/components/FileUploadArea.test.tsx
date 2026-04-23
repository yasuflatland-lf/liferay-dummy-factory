import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {Mock} from 'vitest';

import FileUploadArea from '../../../src/main/resources/META-INF/resources/js/components/FileUploadArea';

type MockFetch = Mock<
	(input: RequestInfo, init?: RequestInit) => Promise<Partial<Response>>
>;

const mockFetch: MockFetch = vi.fn();

beforeEach(() => {
	(global as unknown as {fetch: MockFetch}).fetch = mockFetch;
	mockFetch.mockReset();
});

function renderComponent(
	overrides: Partial<React.ComponentProps<typeof FileUploadArea>> = {}
) {
	const props: React.ComponentProps<typeof FileUploadArea> = {
		groupId: '20121',
		label: 'upload-template-files',
		onChange: vi.fn(),
		testId: 'file-upload',
		uploadURL: '/o/ldf/upload',
		value: '',
		...overrides,
	};

	return {...render(<FileUploadArea {...props} />), props};
}

function uploadFile(container: HTMLElement, fileName: string, content = 'x') {
	const input = container.querySelector(
		'input[type="file"]'
	) as HTMLInputElement;

	const file = new File([content], fileName, {type: 'text/plain'});

	fireEvent.change(input, {target: {files: [file]}});
}

describe('FileUploadArea i18n', () => {
	it('renders the upload-template-files label in its initial state', () => {
		renderComponent();

		expect(
			screen.queryByText(Liferay.Language.get('upload-template-files'))
		).not.toBeNull();
	});

	it('renders the uploading message while a file upload is in flight', async () => {
		let resolveFetch: (value: Partial<Response>) => void = () => undefined;

		mockFetch.mockImplementationOnce(
			() =>
				new Promise<Partial<Response>>((resolve) => {
					resolveFetch = resolve;
				})
		);

		const {container} = renderComponent();

		uploadFile(container, 'template.txt', 'hello');

		const uploadingText = Liferay.Language.get('uploading');

		await waitFor(() => {
			expect(
				screen.queryByText(new RegExp(`^${uploadingText}`))
			).not.toBeNull();
		});

		expect(screen.queryByText(/template\.txt/)).not.toBeNull();

		resolveFetch({
			json: () =>
				Promise.resolve({fileName: 'template.txt', success: true}),
			ok: true,
		} as Partial<Response>);
	});

	it('renders the upload-failed message when the server returns an error', async () => {
		mockFetch.mockResolvedValueOnce({
			ok: false,
			status: 500,
		} as Partial<Response>);

		const {container} = renderComponent();

		uploadFile(container, 'broken.txt', 'fail');

		const errorNode = await waitFor(() => {
			const node = container.querySelector('li.text-danger');
			expect(node).not.toBeNull();
			return node as HTMLElement;
		});

		const errorText = errorNode.textContent ?? '';

		expect(errorText).toContain('broken.txt');
		expect(errorText).toContain('Server error: 500');
	});

	it('falls back to the upload-failed i18n string when the server response is unsuccessful without an error field', async () => {
		mockFetch.mockResolvedValueOnce({
			json: () => Promise.resolve({success: false}),
			ok: true,
		} as Partial<Response>);

		const {container} = renderComponent();

		uploadFile(container, 'noerror.txt', 'fail');

		const uploadFailedText = Liferay.Language.get('upload-failed');

		const errorNode = await waitFor(() => {
			const node = container.querySelector('li.text-danger');
			expect(node).not.toBeNull();
			return node as HTMLElement;
		});

		const errorText = errorNode.textContent ?? '';

		expect(errorText).toContain('noerror.txt');
		expect(errorText).toContain(uploadFailedText);
		expect(uploadFailedText).not.toBe('');
		expect(uploadFailedText).not.toBe('upload-failed');
	});
});
