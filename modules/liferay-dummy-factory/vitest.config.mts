import react from '@vitejs/plugin-react';
import {defineConfig} from 'vitest/config';

export default defineConfig({
	plugins: [react()],
	resolve: {
		dedupe: ['react', 'react-dom'],
	},
	test: {
		coverage: {
			exclude: ['test/**', '**/*.d.ts', '**/types/**', '**/index.ts'],
			include: ['src/main/resources/META-INF/resources/js/**/*.{ts,tsx}'],
			provider: 'v8',
			reporter: ['text', 'lcov'],
			reportsDirectory: './coverage',
		},
		environment: 'jsdom',
		globals: false,
		include: ['test/**/*.test.{ts,tsx}'],
		setupFiles: ['./test/setup.ts'],
	},
});
