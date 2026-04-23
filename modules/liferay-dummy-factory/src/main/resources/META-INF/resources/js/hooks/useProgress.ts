import {useCallback, useEffect, useRef, useState} from 'react';

interface UseProgressResult {
	percent: number;
	reset: () => void;
	running: boolean;
	start: (progressId: string) => void;
}

export function useProgress(
	resourceURL: string | undefined
): UseProgressResult {
	const [percent, setPercent] = useState(0);
	const [running, setRunning] = useState(false);
	const intervalRef = useRef<number | null>(null);

	const stop = useCallback(() => {
		if (intervalRef.current) {
			clearInterval(intervalRef.current);
			intervalRef.current = null;
		}

		setRunning(false);
	}, []);

	const start = useCallback(
		(progressId: string) => {
			if (!resourceURL) {
				return;
			}

			setPercent(0);
			setRunning(true);

			intervalRef.current = window.setInterval(async () => {
				try {
					const response = await fetch(
						`${resourceURL}&progressId=${encodeURIComponent(
							progressId
						)}`,
						{credentials: 'include'}
					);
					const data = await response.json();

					setPercent(data.percent || 0);

					if (data.percent >= 100) {
						stop();
					}
				}
				catch (err) {
					console.warn(
						'Progress polling failed, stopping updates',
						err
					);
					stop();
				}
			}, 1000);
		},
		[resourceURL, stop]
	);

	const reset = useCallback(() => {
		stop();
		setPercent(0);
	}, [stop]);

	useEffect(() => {
		return () => {
			if (intervalRef.current) {
				clearInterval(intervalRef.current);
			}
		};
	}, []);

	return {percent, reset, running, start};
}
