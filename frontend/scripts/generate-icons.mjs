import sharp from 'sharp'
import { mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const publicDirectory = path.resolve(scriptDirectory, '..', 'public')
const input = path.join(publicDirectory, 'app-icon.svg')
const output = path.join(publicDirectory, 'icons')

await mkdir(output, { recursive: true })
await Promise.all(
  [192, 512].map((size) =>
    sharp(input).resize(size, size).png().toFile(path.join(output, `icon-${size}.png`)),
  ),
)
console.log('Generated PWA icons: 192px, 512px')
