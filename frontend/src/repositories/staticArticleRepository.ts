import type { FeedDocument, FeedManifest, SourceHealthDocument } from '../domain/article'
import { loadCachedFeed, saveCachedFeed } from '../services/localDatabase'
import type { ArticleRepository, FeedLoadResult } from './articleRepository'

const SUPPORTED_SCHEMA_MAJOR = '1'

export class IncompatibleSchemaError extends Error {
  constructor(version: string) {
    super(`数据版本 ${version} 与当前应用不兼容，请刷新或升级应用。`)
  }
}

export class StaticArticleRepository implements ArticleRepository {
  private readonly baseUrl: string

  constructor(baseUrl = import.meta.env.BASE_URL) {
    this.baseUrl = baseUrl
  }

  async loadLatest(): Promise<FeedLoadResult> {
    try {
      const manifest = await this.fetchJson<FeedManifest>('data/feed-manifest.json')
      this.assertCompatible(manifest.schemaVersion)
      const response = await fetch(this.resolve('data/latest.json'), { cache: 'no-store' })
      if (!response.ok) throw new Error(`数据请求失败（HTTP ${response.status}）`)
      const bytes = await response.arrayBuffer()
      const expected = manifest.files['latest.json']?.sha256
      if (expected && (await this.sha256(bytes)) !== expected) throw new Error('数据完整性校验失败')
      const feed = JSON.parse(new TextDecoder().decode(bytes)) as FeedDocument
      this.assertCompatible(feed.schemaVersion)
      const sourceHealth = await this.fetchJson<SourceHealthDocument>('data/source-health.json').catch(() => null)
      const cached = { manifest, articles: feed.articles, sourceHealth }
      await saveCachedFeed(cached)
      return { ...cached, fromCache: false }
    } catch (error) {
      if (error instanceof IncompatibleSchemaError) throw error
      const cached = await loadCachedFeed()
      if (cached) return { ...cached, fromCache: true }
      throw error
    }
  }

  private async fetchJson<T>(relativePath: string): Promise<T> {
    const response = await fetch(this.resolve(relativePath), { cache: 'no-store' })
    if (!response.ok) throw new Error(`数据请求失败（HTTP ${response.status}）`)
    return response.json() as Promise<T>
  }

  private resolve(relativePath: string): string {
    return `${this.baseUrl.replace(/\/$/, '')}/${relativePath}`
  }

  private assertCompatible(version: string): void {
    if (version.split('.')[0] !== SUPPORTED_SCHEMA_MAJOR) throw new IncompatibleSchemaError(version)
  }

  private async sha256(value: ArrayBuffer): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', value)
    return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('')
  }
}
