import {useEffect} from 'react';

import {MultiSiteResult, PerSiteResult} from '../types';
import {translate} from '../utils/i18n';

interface ResultAlertProps {
	message: string | null;
	multiSite?: MultiSiteResult | null;
	onDismiss: () => void;
	testId?: string;
	type: 'success' | 'danger' | 'warning';
}

function ResultAlert({
	message,
	multiSite,
	onDismiss,
	testId,
	type,
}: ResultAlertProps) {
	const entries: PerSiteResult[] = multiSite?.perSite ?? [];
	const totalCreated = multiSite?.totalCreated ?? 0;
	const totalRequested = multiSite?.totalRequested ?? 0;

	let effectiveType: 'success' | 'danger' | 'warning' = type;

	if (multiSite) {
		if (multiSite.ok) {
			effectiveType = 'success';
		}
		else if (totalCreated > 0) {
			effectiveType = 'warning';
		}
		else {
			effectiveType = 'danger';
		}
	}

	const hasContent = Boolean(message) || Boolean(multiSite);

	useEffect(() => {
		if (hasContent && effectiveType === 'success') {
			const timer = setTimeout(onDismiss, 5000);

			return () => clearTimeout(timer);
		}
	}, [effectiveType, hasContent, onDismiss]);

	if (!hasContent) {
		return null;
	}

	return (
		<div
			className={`alert alert-${effectiveType} alert-dismissible`}
			data-testid={testId}
			role="alert"
		>
			<button className="close" onClick={onDismiss} type="button">
				<span aria-hidden="true">&times;</span>
			</button>

			{multiSite ? (
				<>
					<div>
						{translate('per-site-results')} {totalCreated}/
						{totalRequested}
					</div>

					<ul className="list-unstyled">
						{entries.map((entry) => (
							<li key={entry.groupId}>
								{entry.siteName ?? entry.groupId}:{' '}
								{entry.created ?? 0}/
								{(entry.created ?? 0) + (entry.failed ?? 0)}
								{entry.error ? ` — ${entry.error}` : ''}
							</li>
						))}
					</ul>
				</>
			) : (
				message
			)}
		</div>
	);
}

export default ResultAlert;
