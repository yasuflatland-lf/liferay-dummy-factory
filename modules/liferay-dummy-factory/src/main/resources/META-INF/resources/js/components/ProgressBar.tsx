interface ProgressBarProps {
	percent: number;
	running: boolean;
	testId?: string;
}

function ProgressBar({percent, running, testId}: ProgressBarProps) {
	if (!running && percent === 0) {
		return null;
	}

	const isComplete = percent === 100;
	const rounded = Math.round(percent);

	return (
		<div className="sheet-section">
			<div
				className={`progress-group${
					isComplete ? ' progress-group-stacked' : ''
				}`}
				data-testid={testId}
			>
				<div className="progress">
					<div
						className={`progress-bar${
							isComplete
								? ' progress-bar-success'
								: ' progress-bar-animated'
						}`}
						role="progressbar"
						style={{width: `${percent}%`}}
					/>
				</div>

				<div className="progress-group-addon">{`${rounded}%`}</div>
			</div>
		</div>
	);
}

export default ProgressBar;
