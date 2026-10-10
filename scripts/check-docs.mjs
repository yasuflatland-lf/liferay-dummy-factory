#!/usr/bin/env node
// Documentation consistency check. Zero dependencies; run from anywhere:
//   node scripts/check-docs.mjs
// Documentation rules: docs/AGENTS.md. Agent-instruction layout rules:
// docs/reference/agent-instructions.md.

import {execFileSync, spawnSync} from 'node:child_process';
import {
	existsSync,
	lstatSync,
	readdirSync,
	readFileSync,
	readlinkSync,
	statSync,
} from 'node:fs';
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
			file.startsWith('.agents/') ||
			file.startsWith('.claude/') ||
			isAgentFile(file))
);

const isAdr = (file) => file.startsWith('docs/adr/');

function isAgentFile(file) {
	return ['AGENTS.md', 'CLAUDE.md'].includes(path.posix.basename(file));
}

// Tool-specific instruction files that only one agent would read.
const TOOL_SPECIFIC_RULES = [
	'.claude/rules',
	'.cursor/rules',
	'.cursorrules',
	'.github/copilot-instructions.md',
];

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
	'.agents/',
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
				candidate.startsWith('.claude/plan') ||
				candidate.startsWith('.agents/plan') ||
				TOOL_SPECIFIC_RULES.some((rule) => candidate.startsWith(rule))
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

// 3. Agent instructions: AGENTS.md is the single source, CLAUDE.md files are
// one-line imports, every scoped AGENTS.md is indexed from the root, the
// chain Codex concatenates fits its default budget, and skills are valid
// Agent Skills exposed to Claude Code through per-skill symlinks.
const CLAUDE_SHIM = '@AGENTS.md';
const CODEX_CHAIN_BUDGET = 32 * 1024;

const agentsFiles = tracked.filter(
	(file) => path.posix.basename(file) === 'AGENTS.md'
);
const rootAgents = readIfExists('AGENTS.md');

if (!rootAgents) {
	report('AGENTS.md', 0, 'root AGENTS.md is missing');
}

for (const file of agentsFiles) {
	const dir = path.posix.dirname(file);
	const shim = dir === '.' ? 'CLAUDE.md' : `${dir}/CLAUDE.md`;

	if (!existsSync(path.join(ROOT, shim))) {
		report(file, 0, `missing sibling ${shim} containing "${CLAUDE_SHIM}"`);
	}

	if (dir !== '.' && !rootAgents.includes(`](${file})`)) {
		report('AGENTS.md', 0, `scoped instructions not indexed: ${file}`);
	}

	let chainBytes = 0;

	for (
		let ancestor = dir;
		;
		ancestor = path.posix.dirname(ancestor)
	) {
		const candidate =
			ancestor === '.' ? 'AGENTS.md' : `${ancestor}/AGENTS.md`;

		if (existsSync(path.join(ROOT, candidate))) {
			chainBytes += statSync(path.join(ROOT, candidate)).size;
		}

		if (ancestor === '.') {
			break;
		}
	}

	if (chainBytes > CODEX_CHAIN_BUDGET) {
		report(
			file,
			0,
			`AGENTS.md chain is ${chainBytes} bytes; Codex truncates after ${CODEX_CHAIN_BUDGET}`
		);
	}
}

for (const file of tracked.filter(
	(file) => path.posix.basename(file) === 'CLAUDE.md'
)) {
	const dir = path.posix.dirname(file);
	const sibling = dir === '.' ? 'AGENTS.md' : `${dir}/AGENTS.md`;

	if (!existsSync(path.join(ROOT, sibling))) {
		report(file, 0, `CLAUDE.md without sibling ${sibling}`);
	}

	if (readIfExists(file).trim() !== CLAUDE_SHIM) {
		report(
			file,
			0,
			`must contain only "${CLAUDE_SHIM}"; put instructions in ${sibling}`
		);
	}
}

for (const rule of TOOL_SPECIFIC_RULES) {
	if (tracked.some((file) => file === rule || file.startsWith(`${rule}/`))) {
		report(rule, 0, 'tool-specific instructions; move them into an AGENTS.md');
	}
}

const SKILLS_DIR = '.agents/skills';
const CLAUDE_SKILLS_DIR = '.claude/skills';

const listDir = (dir) =>
	existsSync(path.join(ROOT, dir)) ? readdirSync(path.join(ROOT, dir)) : [];

const skills = listDir(SKILLS_DIR);

for (const name of skills) {
	const skillFile = `${SKILLS_DIR}/${name}/SKILL.md`;
	const frontMatter = /^---\n([\s\S]*?)\n---/.exec(readIfExists(skillFile));

	if (!frontMatter) {
		report(skillFile, 0, 'missing SKILL.md with YAML front matter');

		continue;
	}

	const field = (key) =>
		new RegExp(`^${key}:\\s*(.*)$`, 'm').exec(frontMatter[1])?.[1].trim();
	const declaredName = field('name');
	const description = field('description');

	if (declaredName !== name) {
		report(skillFile, 0, `name "${declaredName}" must equal directory "${name}"`);
	}

	if (!/^(?!.*--)[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$/.test(name)) {
		report(skillFile, 0, `invalid skill name "${name}" (Agent Skills spec)`);
	}

	if (!description || description.length > 1024) {
		report(skillFile, 0, 'description must be 1-1024 characters');
	}

	if (!rootAgents.includes(`](${skillFile})`)) {
		report('AGENTS.md', 0, `skill not listed: ${skillFile}`);
	}

	const link = `${CLAUDE_SKILLS_DIR}/${name}`;
	const expectedTarget = `../../${SKILLS_DIR}/${name}`;
	const linkStat = existsSync(path.join(ROOT, link))
		? lstatSync(path.join(ROOT, link))
		: null;

	if (
		!linkStat?.isSymbolicLink() ||
		readlinkSync(path.join(ROOT, link)) !== expectedTarget
	) {
		report(link, 0, `must be a symlink to ${expectedTarget}`);
	}
}

for (const name of listDir(CLAUDE_SKILLS_DIR)) {
	if (!skills.includes(name)) {
		report(
			`${CLAUDE_SKILLS_DIR}/${name}`,
			0,
			`no matching ${SKILLS_DIR}/${name}; skills live in ${SKILLS_DIR}`
		);
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
