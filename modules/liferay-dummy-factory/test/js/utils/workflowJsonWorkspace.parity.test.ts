import fs from 'node:fs';
import path from 'node:path';
import {describe, expect, it} from 'vitest';

import {getWorkflowJsonSamples} from '../../../src/main/resources/META-INF/resources/js/utils/workflowJsonWorkspace';

const FIXTURE_DIR = path.resolve(
	__dirname,
	'../../../../../integration-test/src/test/resources/workflow-samples'
);

describe('workflow-samples parity between TS and JSON fixtures', () => {
	it.each(getWorkflowJsonSamples())(
		'$id matches the JSON fixture (deep equal)',
		(sample) => {
			const fixture = JSON.parse(
				fs.readFileSync(path.join(FIXTURE_DIR, `${sample.id}.json`), 'utf8')
			);
			const tsSample = JSON.parse(sample.json);

			expect(tsSample).toEqual(fixture);
		}
	);
});
