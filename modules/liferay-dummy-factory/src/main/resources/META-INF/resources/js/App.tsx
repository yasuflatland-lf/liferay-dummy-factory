import {useState} from 'react';

import {
	APP_TABS,
	AppTab,
	ENTITY_LABELS,
	ENTITY_TYPES,
	EntityType,
	OTHER_ENTITY_TYPES,
} from './config/constants';
import {getEntityConfig} from './config/entities';
import EntityForm from './components/EntityForm';
import EntitySelector from './components/EntitySelector';
import WorkflowJsonEditor from './components/WorkflowJsonEditor';
import {translate} from './utils/i18n';

const WORKFLOW_JSON_RESOURCE_URLS = {
	execute: '/o/ldf-workflow/execute',
	plan: '/o/ldf-workflow/plan',
};

interface AppProps {
	actionResourceURLs: Record<string, string>;
	dataResourceURL: string;
	progressResourceURL: string;
}

function App({
	actionResourceURLs,
	dataResourceURL,
	progressResourceURL,
}: AppProps) {
	const [selectedTab, setSelectedTab] = useState<AppTab>(
		APP_TABS.OTHER_ENTITIES
	);
	const [selectedEntity, setSelectedEntity] = useState<EntityType>(
		ENTITY_TYPES.ORGANIZATION
	);

	const entityConfig = getEntityConfig(selectedEntity);
	const handleWorkflowJsonSampleLoadProxy = () => {
		(
			document.querySelector(
				'[data-testid="workflow-json-load-sample"]'
			) as HTMLButtonElement | null
		)?.click();
	};
	const createEntitiesLabel = translate('create-entities');

	return (
		<>
			<nav className="navbar navbar-collapse-absolute navbar-expand-md navbar-underline navigation-bar navigation-bar-light">
				<div className="container-fluid container-fluid-max-xl">
					<ul className="navbar-nav">
						<li className="nav-item">
							<button
								className={`btn btn-unstyled nav-link ${
									selectedTab === APP_TABS.OTHER_ENTITIES
										? 'active'
										: ''
								}`}
								data-testid="app-tab-create-entities"
								onClick={() =>
									setSelectedTab(APP_TABS.OTHER_ENTITIES)
								}
								type="button"
							>
								<span>{createEntitiesLabel}</span>
							</button>
						</li>
						<li className="nav-item">
							<button
								className={`btn btn-unstyled nav-link ${
									selectedTab === APP_TABS.WORKFLOW_JSON
										? 'active'
										: ''
								}`}
								data-testid="app-tab-workflow-json"
								onClick={() =>
									setSelectedTab(APP_TABS.WORKFLOW_JSON)
								}
								type="button"
							>
								<span>
									{translate(
										ENTITY_LABELS[
											ENTITY_TYPES.WORKFLOW_JSON
										]
									)}
								</span>
							</button>
						</li>
					</ul>
				</div>
			</nav>

			<div className="container-fluid container-fluid-max-xl">
				{selectedTab === APP_TABS.WORKFLOW_JSON ? (
					<div className="position-relative">
						<button
							className="btn btn-link p-0 position-absolute"
							data-testid="workflow-json-sample-load"
							onClick={handleWorkflowJsonSampleLoadProxy}
							style={{
								left: '-9999px',
								top: 0,
							}}
							type="button"
						>
							{translate('load-sample')}
						</button>

						<WorkflowJsonEditor
							executeResourceURL={
								WORKFLOW_JSON_RESOURCE_URLS.execute
							}
							onChange={() => {}}
							planResourceURL={WORKFLOW_JSON_RESOURCE_URLS.plan}
							value=""
						/>
					</div>
				) : (
					<div className="row">
						<div className="col-md-2 pr-0">
							<EntitySelector
								entities={OTHER_ENTITY_TYPES}
								onSelect={setSelectedEntity}
								selected={selectedEntity}
								testId="entity-selector"
							/>
						</div>

						<div className="col-md-10 pl-0">
							{entityConfig ? (
								<EntityForm
									actionResourceURLs={actionResourceURLs}
									config={entityConfig}
									dataResourceURL={dataResourceURL}
									key={selectedEntity}
									progressResourceURL={progressResourceURL}
								/>
							) : (
								<div className="sheet">
									<div className="sheet-section">
										<div className="alert alert-info">
											{translate(
												'this-entity-type-is-not-yet-available'
											)}
										</div>
									</div>
								</div>
							)}
						</div>
					</div>
				)}
			</div>
		</>
	);
}

export default App;
