import type { Article } from '../domain/article'

export function article(overrides: Partial<Article> = {}): Article {
  return {
    id: '50cae2e8-0f53-32d7-8f35-54ec4bd7d65d', sourceId: 'fda', sourceName: 'FDA', sourceTier: 'A',
    sourceType: 'regulator', originalUrl: 'https://example.com/a', canonicalUrl: 'https://example.com/a',
    titleOriginal: 'ExampleDrug receives approval', titleZh: '示例药物获批', summaryZh: '这是用于自动化测试的事实摘要。',
    whyItMattersZh: null, language: 'en', region: 'US', category: 'drug_rd', eventTypes: ['approval'],
    entities: [{ type: 'drug', name: 'ExampleDrug' }], diseaseAreas: ['Rare Disease'],
    publishedAt: '2026-09-06T08:00:00Z', collectedAt: '2026-09-07T00:00:00Z',
    contentHash: 'e43c33f743d40ac126f5e18fb7043c71c254756e10bc5d4ec4e9ee433cb56c4a',
    generated: false, generatorVersion: null, generatedAt: null, verificationStatus: 'auto_checked',
    originalAccessStatus: 'reachable', legalBasis: 'official_api',
    sourceLinks: [{ sourceId: 'fda', sourceName: 'FDA', sourceTier: 'A', originalUrl: 'https://example.com/a' }],
    importanceScore: 90, importanceReasons: ['官方一手来源'], ...overrides,
  }
}
