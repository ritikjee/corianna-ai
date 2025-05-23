// eslint.config.mjs
import js from '@eslint/js';
import tseslint from 'typescript-eslint';

export default [
    js.configs.recommended,
    ...tseslint.configs.recommended,
    {
        files: ['**/*.ts'],
        languageOptions: {
            parser: tseslint.parser,
            parserOptions: {
                sourceType: 'module',
                ecmaVersion: 'latest',
                project: './tsconfig.json',
            },
        },
    },
    {
        files: ['**/*.js'],
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'commonjs',
        },
    },
    {
        ignores: ['dist/**'],
    },
];
