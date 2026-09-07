import { afterEach, describe, expect, it, vi } from 'vitest'
import { StaticArticleRepository } from './staticArticleRepository'
import { article } from '../test/articleFactory'

describe('StaticArticleRepository', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('loads a compatible static feed and source status', async () => {
    const responses = [
      { schemaVersion: '1.0', generatedAt: '2026-09-07T00:00:00Z', latestDate: '2026-09-07', availableDates: [], files: {}, freshness: 'fresh' },
      { schemaVersion: '1.0', generatedAt: '2026-09-07T00:00:00Z', articles: [article()] },
      { schemaVersion: '1.0', generatedAt: '2026-09-07T00:00:00Z', sources: [] },
    ]
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(responses.shift()), { status: 200 })))

    const result = await new StaticArticleRepository('/app/').loadLatest()

    expect(result.articles).toHaveLength(1)
    expect(result.fromCache).toBe(false)
  })

  it('shows an explicit incompatibility instead of using unknown schemas', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ schemaVersion: '2.0' }), { status: 200 })))
    await expect(new StaticArticleRepository('/').loadLatest()).rejects.toThrow(/不兼容/)
  })
})
