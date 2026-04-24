import {describe, expect, it} from 'vitest';

import {ENTITY_CONFIGS} from '../../src/main/resources/META-INF/resources/js/config/entities';
import {ENTITY_TYPES} from '../../src/main/resources/META-INF/resources/js/config/constants';

describe('tags field i18n', () => {
	it.each(['tags', 'tags-help', 'tags-placeholder'])(
		'%s resolves via Language.properties',
		(key) => {
			const text = Liferay.Language.get(key);
			expect(text).not.toBe(key);
			expect(text.length).toBeGreaterThan(0);
		}
	);
});

describe('tags field presence in entity configs', () => {
	it.each([
		ENTITY_TYPES.WCM,
		ENTITY_TYPES.DOCUMENTS,
		ENTITY_TYPES.MB_THREAD,
		ENTITY_TYPES.MB_REPLY,
	])('%s config contains a tags textarea field', (entityType) => {
		const config = ENTITY_CONFIGS[entityType];
		expect(config).toBeDefined();
		const tagsField = config!.fields.find((f) => f.name === 'tags');
		expect(tagsField).toBeDefined();
		expect(tagsField!.type).toBe('textarea');
	});
});
