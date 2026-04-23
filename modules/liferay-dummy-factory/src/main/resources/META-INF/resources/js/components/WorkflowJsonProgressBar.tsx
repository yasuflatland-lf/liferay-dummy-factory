export interface WorkflowJsonProgressBarProps {
	busy: boolean;
}

function WorkflowJsonProgressBar({busy}: WorkflowJsonProgressBarProps) {
	if (!busy) {
		return null;
	}

	return (
		<div
			aria-hidden="true"
			className="progress progress-sm progress-indeterminate"
			data-testid="workflow-json-progress"
		>
			<div className="progress-bar" />
		</div>
	);
}

export default WorkflowJsonProgressBar;
