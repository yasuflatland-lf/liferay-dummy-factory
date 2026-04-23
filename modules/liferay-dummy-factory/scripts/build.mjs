import crypto from 'crypto';
import esbuild from 'esbuild';
import fs from 'fs';
import path from 'path';
import {fileURLToPath} from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_DIR = path.resolve(__dirname, '..');
const OUTPUT_DIR = path.resolve(
	PROJECT_DIR,
	'classes',
	'META-INF',
	'resources'
);
const LIFERAY_DIR = path.join(OUTPUT_DIR, '__liferay__');

function getWebContextPath() {
	const bndContent = fs.readFileSync(
		path.join(PROJECT_DIR, 'bnd.bnd'),
		'utf8'
	);
	const match = bndContent.match(/^Web-ContextPath:\s*(.+)$/m);

	return match ? match[1].trim() : '';
}

async function getProjectConfig() {
	const configPath = path.join(PROJECT_DIR, 'node-scripts.config.js');

	if (fs.existsSync(configPath)) {
		const configURL = new URL(`file://${configPath}`);
		const module = await import(configURL);

		return module.default;
	}

	return {main: './src/main/resources/META-INF/resources/js/index.ts'};
}

function hashPathForVariable(filePath) {
	const normalizedFilePath = filePath.split(path.sep).join('');

	return (
		'a' +
		crypto.createHash('sha256').update(normalizedFilePath).digest('hex')
	);
}

const liferayReactExternalsPlugin = {
	name: 'liferay-react-externals',
	setup(build) {
		const externals = {
			'react':
				'../../frontend-js-react-web/__liferay__/exports/react.js',
			'react-dom':
				'../../frontend-js-react-web/__liferay__/exports/react-dom.js',
		};

		for (const [pkg, url] of Object.entries(externals)) {
			build.onResolve(
				{filter: new RegExp(`^${pkg.replace('/', '\\/')}$`)},
				() => ({
					external: true,
					path: url,
				})
			);
		}
	},
};

async function main() {
	const pkgJson = JSON.parse(
		fs.readFileSync(path.join(PROJECT_DIR, 'package.json'), 'utf8')
	);
	const config = await getProjectConfig();
	const webContextPath = getWebContextPath();
	const entryPoint = path.resolve(PROJECT_DIR, config.main);

	fs.mkdirSync(LIFERAY_DIR, {recursive: true});

	// 1. Bundle with esbuild

	await esbuild.build({
		banner: {
			js: 'import * as React from "../../frontend-js-react-web/__liferay__/exports/react.js";',
		},
		bundle: true,
		entryPoints: [entryPoint],
		format: 'esm',
		jsx: 'transform',
		outfile: path.join(LIFERAY_DIR, 'index.js'),
		plugins: [liferayReactExternalsPlugin],
		sourcemap: true,
		target: 'es2020',
	});

	// 2. Generate AMD bridge (mirrors createEsm2AmdIndexBridge)

	const importPath = `../../../${webContextPath.replace(/^\//, '')}/__liferay__/index.js`;
	const hashedVar = hashPathForVariable(importPath);

	const bridgeSource = [
		`import * as ${hashedVar} from "${importPath}";`,
		'',
		'Liferay.Loader.define(',
		`\t"${pkgJson.name}@${pkgJson.version}/index",`,
		"\t['module'],",
		'\tfunction (module) {',
		`\t\tmodule.exports = ${hashedVar};`,
		'\t}',
		');',
		'',
	].join('\n');

	fs.writeFileSync(path.join(OUTPUT_DIR, 'index.js'), bridgeSource, 'utf8');

	// 3. Write output package.json

	const outPkgJson = {
		main: 'index.js',
		name: pkgJson.name,
		version: pkgJson.version,
	};

	fs.writeFileSync(
		path.join(OUTPUT_DIR, 'package.json'),
		JSON.stringify(outPkgJson, null, '\t') + '\n',
		'utf8'
	);

	// 4. Write manifest.json

	const manifest = {
		packages: {
			'/': {
				dest: {
					dir: '.',
					id: '/',
					name: pkgJson.name,
					version: pkgJson.version,
				},
				modules: {
					'index.js': {flags: {esModule: true, useESM: true}},
				},
				src: {
					id: '/',
					name: pkgJson.name,
					version: pkgJson.version,
				},
			},
		},
	};

	fs.writeFileSync(
		path.join(OUTPUT_DIR, 'manifest.json'),
		JSON.stringify(manifest, null, '\t') + '\n',
		'utf8'
	);

	console.log('Build complete:', OUTPUT_DIR);
}

main().catch((err) => {
	console.error(err);
	process.exit(1);
});
