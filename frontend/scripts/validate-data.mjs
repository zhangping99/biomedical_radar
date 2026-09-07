import Ajv2020 from 'ajv/dist/2020.js'
import addFormats from 'ajv-formats'
import { createHash } from 'node:crypto'
import { readFile, readdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDirectory = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repositoryRoot = path.resolve(frontendDirectory, '..')
const contractsDirectory = path.join(repositoryRoot, 'contracts')
const fixtureDirectory = path.join(repositoryRoot, 'data', 'fixtures', 'contracts')
const dataDirectory = process.env.RADAR_DATA_DIR
  ? path.resolve(frontendDirectory, process.env.RADAR_DATA_DIR)
  : path.join(frontendDirectory, 'public', 'data')

const readJson = async (file) => JSON.parse(await readFile(file, 'utf8'))
const articleSchema = await readJson(path.join(contractsDirectory, 'article.schema.json'))
const feedSchema = await readJson(path.join(contractsDirectory, 'feed.schema.json'))
const healthSchema = await readJson(path.join(contractsDirectory, 'source-health.schema.json'))

const ajv = new Ajv2020({ allErrors: true, strict: true })
addFormats(ajv)
ajv.addSchema(articleSchema)
const validateArticle = ajv.getSchema(articleSchema.$id)
const validateFeed = ajv.compile(feedSchema)
const validateHealth = ajv.compile(healthSchema)

const assertValid = (label, validator, value) => {
  if (!validator(value)) throw new Error(`${label} validation failed: ${ajv.errorsText(validator.errors)}`)
}

assertValid('valid Article fixture', validateArticle, await readJson(path.join(fixtureDirectory, 'article.valid.json')))
const invalidFixture = await readJson(path.join(fixtureDirectory, 'article.invalid.json'))
if (validateArticle(invalidFixture)) throw new Error('Invalid Article fixture was unexpectedly accepted')
assertValid('valid Feed fixture', validateFeed, await readJson(path.join(fixtureDirectory, 'feed.valid.json')))

const manifest = await readJson(path.join(dataDirectory, 'feed-manifest.json'))
assertValid('feed-manifest.json', validateFeed, manifest)
assertValid('latest.json', validateFeed, await readJson(path.join(dataDirectory, 'latest.json')))
assertValid('source-health.json', validateHealth, await readJson(path.join(dataDirectory, 'source-health.json')))

const dailyDirectory = path.join(dataDirectory, 'daily')
for (const file of await readdir(dailyDirectory)) {
  if (file.endsWith('.json')) assertValid(`daily/${file}`, validateFeed, await readJson(path.join(dailyDirectory, file)))
}

for (const [relativePath, metadata] of Object.entries(manifest.files)) {
  const content = await readFile(path.join(dataDirectory, relativePath))
  const actual = createHash('sha256').update(content).digest('hex')
  if (actual !== metadata.sha256) throw new Error(`SHA-256 mismatch: ${relativePath}`)
  if (content.length !== metadata.sizeBytes) throw new Error(`Size mismatch: ${relativePath}`)
}

console.log(`Validated contracts and ${Object.keys(manifest.files).length + 1} exported files`)
