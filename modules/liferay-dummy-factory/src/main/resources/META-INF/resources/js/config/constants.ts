export const ENTITY_TYPES = {
	WORKFLOW_JSON: 'WORKFLOW_JSON',
	ORGANIZATION: 'ORG',
	ROLES: 'ROLES',
	USERS: 'USERS',
	WCM: 'WCM',
	DOCUMENTS: 'DOC',
	PAGES: 'PAGES',
	SITES: 'SITES',
	CATEGORY: 'CATEGORY',
	VOCABULARY: 'VOCABULARY',
	COMPANY: 'COMPANY',
	MB_CATEGORY: 'MB_CATEGORY',
	MB_REPLY: 'MB_REPLY',
	MB_THREAD: 'MB_THREAD',
	BLOGS: 'BLOGS',
} as const;

export type EntityType = (typeof ENTITY_TYPES)[keyof typeof ENTITY_TYPES];

export const APP_TABS = {
	OTHER_ENTITIES: 'OTHER_ENTITIES',
	WORKFLOW_JSON: 'WORKFLOW_JSON',
} as const;

export type AppTab = (typeof APP_TABS)[keyof typeof APP_TABS];

const entityTypes = Object.values(ENTITY_TYPES) as EntityType[];

export const OTHER_ENTITY_TYPES = entityTypes.filter(
	(entityType) => entityType !== ENTITY_TYPES.WORKFLOW_JSON
);

export const ENTITY_LABELS: Record<EntityType, string> = {
	WORKFLOW_JSON: 'workflow-json',
	BLOGS: 'blogs',
	CATEGORY: 'categories',
	COMPANY: 'company',
	DOC: 'documents',
	MB_CATEGORY: 'mb-categories',
	MB_REPLY: 'mb-replies',
	MB_THREAD: 'mb-threads',
	ORG: 'organizations',
	PAGES: 'pages',
	ROLES: 'roles',
	SITES: 'sites',
	USERS: 'users',
	VOCABULARY: 'vocabularies',
	WCM: 'web-content',
};

export const ENTITY_DISPLAY_LABELS: Partial<Record<EntityType, string>> = {
	WORKFLOW_JSON: 'Workflow JSON',
};
