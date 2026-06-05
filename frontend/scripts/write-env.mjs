import { existsSync } from 'node:fs';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDir, '..');
const envPath = resolve(projectRoot, '.env');
const outputPath = resolve(projectRoot, 'public/env.js');

function parseEnv(contents) {
  return contents
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#'))
    .reduce((values, line) => {
      const separatorIndex = line.indexOf('=');

      if (separatorIndex === -1) {
        return values;
      }

      const key = line.slice(0, separatorIndex).trim();
      const rawValue = line.slice(separatorIndex + 1).trim();
      const value = rawValue.replace(/^['"]|['"]$/g, '');

      return { ...values, [key]: value };
    }, {});
}

const fileValues = existsSync(envPath)
  ? parseEnv(await readFile(envPath, 'utf8'))
  : {};

const backendUrl = (
  process.env.BACKEND_URL
  ?? process.env.NG_APP_BACKEND_URL
  ?? fileValues.BACKEND_URL
  ?? fileValues.NG_APP_BACKEND_URL
  ?? 'http://localhost:8080'
).replace(/\/+$/, '');

await mkdir(dirname(outputPath), { recursive: true });
const contents = `window.__farmcastEnv = {
  backendUrl: ${JSON.stringify(backendUrl)}
};
`;

if (!existsSync(outputPath) || await readFile(outputPath, 'utf8') !== contents) {
  await writeFile(outputPath, contents);
}
