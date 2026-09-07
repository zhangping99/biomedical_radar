import type { Article, FeedManifest, SourceHealthDocument } from '../domain/article'

export interface FeedLoadResult {
  manifest: FeedManifest
  articles: Article[]
  sourceHealth: SourceHealthDocument | null
  fromCache: boolean
}

export interface ArticleRepository {
  loadLatest(): Promise<FeedLoadResult>
}
