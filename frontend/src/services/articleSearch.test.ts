import { describe, expect, it } from 'vitest'
import { emptyFilters, filterArticles, searchArticles } from './articleSearch'
import { article } from '../test/articleFactory'

describe('article search and filters', () => {
  const records = [
    article(),
    article({ id: '50cae2e8-0f53-32d7-8f35-54ec4bd7d66e', region: 'CN', category: 'quality_safety',
      sourceTier: 'B', titleOriginal: '中国药品安全提示', titleZh: '中国药品安全提示',
      eventTypes: ['safety_alert'], entities: [], diseaseAreas: [], importanceScore: 70 }),
  ]

  it('matches titles, entities and disease areas case-insensitively', () => {
    expect(searchArticles(records, 'exampledrug')).toHaveLength(1)
    expect(searchArticles(records, 'RARE DISEASE')).toHaveLength(1)
    expect(searchArticles(records, '')).toEqual([])
  })

  it('uses OR within a group and AND between groups', () => {
    const filters = emptyFilters()
    filters.regions = ['CN', 'EU']
    filters.categories = ['quality_safety']
    expect(filterArticles(records, filters).map((record) => record.region)).toEqual(['CN'])
  })
})
