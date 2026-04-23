import {useCallback, useEffect, useState} from 'react';

import {SelectOption} from '../types';
import {fetchResource} from '../utils/api';

interface UseApiDataResult {
	data: SelectOption[];
	error: string | null;
	loading: boolean;
}

export function useApiData(
	resourceURL: string | undefined,
	dataSource: string | undefined,
	extraParams?: Record<string, string>
): UseApiDataResult {
	const [data, setData] = useState<SelectOption[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const extraParamsKey = extraParams ? JSON.stringify(extraParams) : '';

	const load = useCallback(async () => {
		if (!resourceURL || !dataSource) {
			return;
		}

		setLoading(true);
		setError(null);

		const params: Record<string, string> = {
			type: dataSource.split('/').pop() || '',
			...(extraParams || {}),
		};

		const result = await fetchResource<SelectOption[]>(resourceURL, params);

		if (result.success) {
			setData(result.data);
		}
		else {
			setError(result.error);
		}

		setLoading(false);
	}, [resourceURL, dataSource, extraParamsKey]);

	useEffect(() => {
		load();
	}, [load]);

	return {data, error, loading};
}
