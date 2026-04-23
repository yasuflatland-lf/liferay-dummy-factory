export type FieldType =
	| 'number'
	| 'text'
	| 'file'
	| 'select'
	| 'multiselect'
	| 'toggle'
	| 'textarea';

export const FIELD_GROUPS = [
	'identity',
	'generator',
	'membership',
	'layout',
	'content',
] as const;
export type FieldGroup = (typeof FIELD_GROUPS)[number];

export interface SelectOption {
	label: string;
	value: string;
}

export interface FieldDefinition {
	advanced?: boolean;
	dataSource?: string;
	group?: FieldGroup;
	defaultValue?: unknown;
	dependsOn?: {
		field: string;
		paramName: string;
	};
	label: string;
	name: string;
	options?: SelectOption[];
	required: boolean;
	type: FieldType;
	validators?: Validator[];
	visibleWhen?: {
		field: string;
		value: string | string[];
	};
}

export interface PerSiteResult {
	groupId: number;
	siteName: string;
	created: number;
	failed: number;
	error?: string;
}

export interface MultiSiteResult {
	ok: boolean;
	totalRequested: number;
	totalCreated: number;
	perSite: PerSiteResult[];
}

export interface Validator {
	message: string;
	type: 'min' | 'max' | 'required' | 'digits';
	value?: number;
}

export interface EntityFormConfig {
	actionURL: string;
	entityType: string;
	fields: FieldDefinition[];
	helpText: string;
	icon: string;
	label: string;
}

export type ApiResponse<T = unknown> =
	| {success: true; data: T}
	| {success: false; data?: T; error: string};

export interface WorkflowValidationError {
	code: string;
	message: string;
	path: string;
}

export interface WorkflowOnErrorPolicy {
	policy: string;
}

export interface WorkflowParameter {
	from?: string;
	name: string;
	value?: unknown;
}

export interface WorkflowStepInput {
	id: string;
	idempotencyKey: string;
	onError?: WorkflowOnErrorPolicy;
	operation: string;
	params?: WorkflowParameter[];
}

export interface WorkflowRequestPayload {
	input?: Record<string, unknown>;
	schemaVersion: string;
	steps: WorkflowStepInput[];
	workflowId?: string;
}

export interface WorkflowPlanResponse {
	errors: WorkflowValidationError[];
	plan?: Record<string, unknown> | null;
}

export interface WorkflowExecuteResponse {
	errors: WorkflowValidationError[];
	execution?: Record<string, unknown> | null;
}

