import {
	ENTITY_DISPLAY_LABELS,
	ENTITY_LABELS,
	EntityType,
} from '../config/constants';
import {translate} from '../utils/i18n';

interface EntitySelectorProps {
	entities: EntityType[];
	onSelect: (entityType: EntityType) => void;
	selected: EntityType;
	testId?: string;
}

function EntitySelector({
	entities,
	onSelect,
	selected,
	testId,
}: EntitySelectorProps) {
	const getLabel = (entityType: EntityType) =>
		ENTITY_DISPLAY_LABELS[entityType] ??
		translate(ENTITY_LABELS[entityType]);

	return (
		<nav
			className="menubar menubar-transparent menubar-vertical-expand-md"
			data-testid={testId}
		>
			<ul className="nav nav-nested">
				{entities.map((entityType) => (
					<li className="nav-item" key={entityType}>
						<button
							className={`btn btn-unstyled nav-link ${
								selected === entityType ? 'active' : ''
							}`}
							data-testid={`entity-selector-${entityType}`}
							onClick={() => onSelect(entityType)}
							type="button"
						>
							{getLabel(entityType)}
						</button>
					</li>
				))}
			</ul>
		</nav>
	);
}

export default EntitySelector;
