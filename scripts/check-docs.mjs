#!/usr/bin/env node
// Documentation consistency check. Zero dependencies; run from anywhere:
//   node scripts/check-docs.mjs
// Rules enforced are described in .claude/rules/documentation.md.

import {execFileSync, spawnSync} from 'node:child_process';
import {existsSync, readFileSync, statSync} from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');

const tracked = execFileSync('git', ['ls-files', '-co', '--exclude-standard'], {
	cwd: ROOT,
	encoding: 'utf8',
})
	.split('\n')
	.filter((file) => file && existsSync(path.join(ROOT, file)));

const markdownFiles = tracked.filter(
	(file) =>
		file.endsWith('.md') &&
		!file.includes('node_modules/') &&
		(!file.includes('/') ||
			file.startsWith('docs/') ||
			file.startsWith('.claude/'))
);

const isAdr = (file) => file.startsWith('docs/adr/');

const errors = [];

const report = (file, line, message) =>
	errors.push(`${file}${line ? `:${line}` : ''}: ${message}`);

// GitHub heading anchors.
function slugify(heading) {
	return heading
		.trim()
		.toLowerCase()
		.replace(/<[^>]+>/g, '')
		.replace(/[^\p{L}\p{N}\s_-]/gu, '')
		.replace(/\s/g, '-');
}

const anchorCache = new Map();

function anchorsOf(file) {
	if (!anchorCache.has(file)) {
		const anchors = new Set();
		const counts = new Map();

		for (const {text} of proseLines(file)) {
			const match = /^#{1,6}\s+(.*?)\s*#*\s*$/.exec(text);

			if (!match) {
				continue;
			}

			const base = slugify(match[1]);
			const count = counts.get(base) ?? 0;

			anchors.add(count ? `${base}-${count}` : base);
			counts.set(base, count + 1);
		}

		anchorCache.set(file, anchors);
	}

	return anchorCache.get(file);
}

// Lines outside fenced code blocks and YAML front matter.
function proseLines(file) {
	const lines = readFileSync(path.join(ROOT, file), 'utf8').split('\n');
	const result = [];
	let fence = null;
	let frontMatter = lines[0] === '---';

	lines.forEach((text, index) => {
		if (frontMatter) {
			if (index > 0 && text === '---') {
				frontMatter = false;
			}

			return;
		}

		const fenceMatch = /^\s*(```+|~~~+)/.exec(text);

		if (fenceMatch) {
			if (!fence) {
				fence = fenceMatch[1];
			}
			else if (fenceMatch[1].startsWith(fence)) {
				fence = null;
			}

			return;
		}

		if (!fence) {
			result.push({line: index + 1, text});
		}
	});

	return result;
}

// 1. Relative links and anchors resolve.
for (const file of markdownFiles) {
	for (const {line, text} of proseLines(file)) {
		const withoutCode = text.replace(/`[^`]*`/g, '');

		for (const match of withoutCode.matchAll(/\[[^\]]*\]\(([^)\s]+)\)/g)) {
			const target = match[1];

			if (/^[a-z][a-z0-9+.-]*:/i.test(target)) {
				continue;
			}

			const [targetPath, anchor] = target.split('#');
			const resolved = targetPath
				? path.posix.normalize(
						path.posix.join(path.posix.dirname(file), targetPath)
					)
				: file;

			if (!existsSync(path.join(ROOT, resolved))) {
				report(file, line, `broken link: ${target}`);

				continue;
			}

			if (
				anchor &&
				resolved.endsWith('.md') &&
				!anchorsOf(resolved).has(anchor)
			) {
				report(file, line, `missing anchor: ${target}`);
			}
		}
	}
}

// 2. Repository paths quoted in living docs exist. ADRs are historical records.
const TOP_LEVEL = [
	'.claude/',
	'.github/',
	'configs/',
	'docs/',
	'gradle/',
	'integration-test/',
	'latest/',
	'modules/',
	'scripts/',
];

for (const file of markdownFiles.filter((file) => !isAdr(file))) {
	for (const {line, text} of proseLines(file)) {
		for (const match of text.matchAll(/`([^`\s]+)`/g)) {
			const candidate = match[1].replace(/[:#].*$/, '').replace(/\/$/, '');

			if (
				!TOP_LEVEL.some((prefix) => candidate.startsWith(prefix)) ||
				/[<>*{}$…]/.test(candidate) ||
				candidate.startsWith('.claude/plan')
			) {
				continue;
			}

			const gitIgnored =
				spawnSync('git', ['check-ignore', '-q', candidate], {cwd: ROOT})
					.status === 0;

			if (!gitIgnored && !existsSync(path.join(ROOT, candidate))) {
				report(file, line, `path does not exist: ${candidate}`);
			}
		}
	}
}

// 3. Every `paths:` glob of a path-scoped rule matches at least one file.
function globToRegExp(glob) {
	let source = '';

	for (let i = 0; i < glob.length; i++) {
		const char = glob[i];

		if (char === '*' && glob[i + 1] === '*') {
			source += glob[i + 2] === '/' ? '(?:.*/)?' : '.*';
			i += glob[i + 2] === '/' ? 2 : 1;
		}
		else if (char === '*') {
			source += '[^/]*';
		}
		else if (char === '{') {
			const end = glob.indexOf('}', i);

			source += `(?:${glob
				.slice(i + 1, end)
				.split(',')
				.map((part) => part.replace(/[.+?^$()|[\]\\]/g, '\\$&'))
				.join('|')})`;
			i = end;
		}
		else {
			source += char.replace(/[.+?^$()|[\]\\]/g, '\\$&');
		}
	}

	return new RegExp(`^${source}$`);
}

for (const file of markdownFiles.filter((file) =>
	file.startsWith('.claude/rules/')
)) {
	const content = readFileSync(path.join(ROOT, file), 'utf8');
	const frontMatter = /^---\n([\s\S]*?)\n---/.exec(content);

	if (!frontMatter) {
		continue;
	}

	for (const match of frontMatter[1].matchAll(/^\s*-\s*["']?([^"'\n]+)["']?\s*$/gm)) {
		const regExp = globToRegExp(match[1].trim());

		if (!tracked.some((trackedFile) => regExp.test(trackedFile))) {
			report(file, 0, `paths glob matches no file: ${match[1]}`);
		}
	}
}

// 4. The ADR index lists every ADR.
const adrIndex = 'docs/adr/README.md';

if (existsSync(path.join(ROOT, adrIndex))) {
	const index = readFileSync(path.join(ROOT, adrIndex), 'utf8');

	for (const file of tracked.filter((file) =>
		/^docs\/adr\/\d{4}-.*\.md$/.test(file)
	)) {
		if (!index.includes(`(${path.basename(file)})`)) {
			report(adrIndex, 0, `ADR not listed: ${path.basename(file)}`);
		}
	}
}

// 5. Tool versions are read from their pin files, never copied into living docs.
function readIfExists(file) {
	const absolute = path.join(ROOT, file);

	return existsSync(absolute) && statSync(absolute).isFile()
		? readFileSync(absolute, 'utf8')
		: '';
}

const pinnedVersions = new Set();

for (const match of readIfExists('gradle.properties').matchAll(
	/^test\.\w+\.version=(.+)$/gm
)) {
	pinnedVersions.add(match[1].trim());
}

for (const match of readIfExists('settings.gradle').matchAll(
	/version:\s*"([^"]+)"/g
)) {
	pinnedVersions.add(match[1]);
}

const modulePackage = JSON.parse(
	readIfExists('modules/liferay-dummy-factory/package.json') || '{}'
);

for (const version of Object.values({
	...modulePackage.dependencies,
	...modulePackage.devDependencies,
})) {
	if (/^\d+\.\d+\.\d+/.test(version)) {
		pinnedVersions.add(version);
	}
}

for (const file of markdownFiles.filter((file) => !isAdr(file))) {
	for (const {line, text} of proseLines(file)) {
		for (const version of pinnedVersions) {
			const pattern = new RegExp(
				`(^|[^\\w.])${version.replace(/[.+-]/g, '\\$&')}(?!\\w|\\.\\d)`
			);

			if (pattern.test(text)) {
				report(
					file,
					line,
					`pinned version ${version} copied into prose; point to the pin file instead`
				);
			}
		}
	}
}

if (errors.length) {
	console.error(errors.join('\n'));
	console.error(`\n${errors.length} documentation problem(s) found.`);
	process.exit(1);
}

console.log(`Documentation OK (${markdownFiles.length} files checked).`);
