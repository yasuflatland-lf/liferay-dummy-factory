module.exports = {
	bracketSpacing: false,
	endOfLine: 'lf',
	jsxSingleQuote: false,
	plugins: ['@liferay/prettier-plugin'],
	overrides: [
		{
			files: 'package.json',
			options: {tabWidth: 2, useTabs: false},
		},
	],
	quoteProps: 'consistent',
	singleQuote: true,
	tabWidth: 4,
	trailingComma: 'es5',
	useTabs: true,
};
