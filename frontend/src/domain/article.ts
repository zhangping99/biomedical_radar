export const categories = [
  'policy_regulation', 'drug_rd', 'research_academic', 'pharma_biotech',
  'hospital_service', 'device_diagnostics', 'capital_transactions', 'reimbursement_access',
  'manufacturing_supply', 'quality_safety', 'public_health', 'frontier_technology',
] as const

export type Category = (typeof categories)[number]
export type Region = 'CN' | 'US' | 'EU' | 'JP' | 'GLOBAL' | 'OTHER'
export type SourceTier = 'A' | 'B' | 'C'
export type SourceType = 'regulator' | 'registry' | 'journal' | 'company' | 'exchange' | 'media'

export interface EntityRef {
  type: 'company' | 'drug' | 'disease' | 'hospital' | 'institution' | 'person' | 'other'
  name: string
}

export interface SourceLink {
  sourceId: string
  sourceName: string
  sourceTier: SourceTier
  originalUrl: string
}

export interface Article {
  id: string
  sourceId: string
  sourceName: string
  sourceTier: SourceTier
  sourceType: SourceType
  originalUrl: string
  canonicalUrl: string
  titleOriginal: string
  titleZh: string | null
  summaryZh: string | null
  whyItMattersZh: string | null
  language: string
  region: Region
  category: Category
  eventTypes: string[]
  entities: EntityRef[]
  diseaseAreas: string[]
  publishedAt: string | null
  collectedAt: string
  contentHash: string
  generated: boolean
  generatorVersion: string | null
  generatedAt: string | null
  verificationStatus: 'unreviewed' | 'auto_checked' | 'human_verified' | 'rejected'
  originalAccessStatus: 'unknown' | 'reachable' | 'slow' | 'unreachable'
  legalBasis: 'official_api' | 'rss_allowed' | 'public_notice' | 'licensed' | 'manual_link'
  sourceLinks: SourceLink[]
  importanceScore: number
  importanceReasons: string[]
}

export interface FeedDocument {
  schemaVersion: string
  generatedAt: string
  articles: Article[]
}

export interface FeedManifest {
  schemaVersion: string
  generatedAt: string
  latestDate: string
  availableDates: string[]
  files: Record<string, { sha256: string; sizeBytes: number }>
  freshness: 'fresh' | 'stale' | 'partial' | 'failed'
}

export interface SourceHealthItem {
  sourceId: string
  sourceName: string
  status: 'ok' | 'failed'
  completedAt: string
  fetchedCount: number
  failedCount: number
  errorCode: string | null
}

export interface SourceHealthDocument {
  schemaVersion: string
  generatedAt: string
  sources: SourceHealthItem[]
}

export const categoryLabels: Record<Category, string> = {
  policy_regulation: '政策监管',
  drug_rd: '药物研发',
  research_academic: '科研学术',
  pharma_biotech: '医药生物',
  hospital_service: '医院服务',
  device_diagnostics: '器械诊断',
  capital_transactions: '资本交易',
  reimbursement_access: '医保准入',
  manufacturing_supply: '生产供应',
  quality_safety: '质量安全',
  public_health: '公共卫生',
  frontier_technology: '前沿技术',
}

export const regionLabels: Record<Region, string> = {
  CN: '中国', US: '美国', EU: '欧盟', JP: '日本', GLOBAL: '全球', OTHER: '其他',
}

export const eventLabels: Record<string, string> = {
  approval: '获批', safety_alert: '安全警示', recall: '召回', phase_3_result: '三期结果',
  policy_release: '政策发布', clinical_registration: '临床登记', filing: '企业公告',
  research_publication: '研究发表', update: '动态',
}

export function articleTimestamp(article: Article): number {
  return Date.parse(article.publishedAt ?? article.collectedAt)
}

export function displayTitle(article: Article): string {
  return article.titleZh || article.titleOriginal
}
