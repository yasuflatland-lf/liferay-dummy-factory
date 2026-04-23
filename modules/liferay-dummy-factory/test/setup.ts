import {cleanup} from '@testing-library/react';
import {readFileSync} from 'fs';
import {resolve} from 'path';
import {afterEach} from 'vitest';

const __dirname = import.meta.dirname;

const languagePropertiesPath = resolve(
	__dirname,
	'../src/main/resources/content/Language.properties'
);

function loadLanguageProperties(path: string): Map<string, string> {
	const map = new Map<string, string>();
	const content = readFileSync(path, 'utf-8');

	for (const rawLine of content.split(/\r?\n/)) {
		const line = rawLine.trim();
		if (line === '' || line.startsWith('#')) {
			continue;
		}
		const eqIndex = line.indexOf('=');
		if (eqIndex === -1) {
			continue;
		}
		const key = line.slice(0, eqIndex).trim();
		const value = line.slice(eqIndex + 1).trim();
		map.set(key, value);
	}

	return map;
}

const languageMap = loadLanguageProperties(languagePropertiesPath);

(globalThis as any).Liferay = {
	authToken: 'test-auth-token',
	Language: {
		get(key: string): string {
			return languageMap.get(key) ?? key;
		},
	},
};

afterEach(() => {
	cleanup();
});
