import { beforeEach, describe, expect, it } from 'vitest'
import { addFollow, clearUserData, exportUserData, listFavorites, listFollows, parseBackup, setFavorite } from './localDatabase'
import { article } from '../test/articleFactory'

describe('local personal data', () => {
  beforeEach(async () => clearUserData())

  it('stores a minimal favorite snapshot and merges stable follow ids', async () => {
    await setFavorite(article(), true)
    await addFollow('drug', 'ExampleDrug')
    await addFollow('drug', 'ExampleDrug')

    expect(await listFavorites()).toHaveLength(1)
    expect((await listFavorites())[0]?.snapshot.title).toBe('示例药物获批')
    expect(await listFollows()).toHaveLength(1)
    expect((await exportUserData()).backupVersion).toBe(1)
  })

  it('rejects unknown backup versions', () => {
    expect(() => parseBackup('{"backupVersion":2,"favorites":[],"follows":[]}')).toThrow(/backupVersion=1/)
  })
})
