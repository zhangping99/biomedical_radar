import { articleTimestamp, displayTitle, type Article, type Category, type Region, type SourceTier } from '../domain/article'

export interface ArticleFilters {
  regions: Region[]
  categories: Category[]
  events: string[]
  diseases: string[]
  sourceTiers: SourceTier[]
  sort: 'newest' | 'importance'
}

export const emptyFilters = (): ArticleFilters => ({
  regions: [], categories: [], events: [], diseases: [], sourceTiers: [], sort: 'newest',
})

export function searchArticles(articles: Article[], rawQuery: string): Article[] {
  const query = rawQuery.trim().toLocaleLowerCase()
  if (!query) return []
  return articles.filter((article) => searchableText(article).includes(query))
}

export function filterArticles(articles: Article[], filters: ArticleFilters): Article[] {
  const filtered = articles.filter((article) =>
    matchesGroup(filters.regions, article.region) &&
    matchesGroup(filters.categories, article.category) &&
    matchesAny(filters.events, article.eventTypes) &&
    matchesAny(filters.diseases, article.diseaseAreas) &&
    matchesGroup(filters.sourceTiers, article.sourceTier),
  )
  return filtered.sort(filters.sort === 'importance'
    ? (a, b) => b.importanceScore - a.importanceScore || articleTimestamp(b) - articleTimestamp(a)
    : (a, b) => articleTimestamp(b) - articleTimestamp(a))
}

export function matchesFollow(article: Article, keyword: string): boolean {
  return searchableText(article).includes(keyword.trim().toLocaleLowerCase())
}

function searchableText(article: Article): string {
  return [
    displayTitle(article), article.titleOriginal, article.summaryZh, article.sourceName,
    ...article.entities.map((entity) => entity.name), ...article.diseaseAreas, ...article.eventTypes,
  ].filter(Boolean).join(' ').toLocaleLowerCase()
}

function matchesGroup<T>(selected: T[], actual: T): boolean {
  return selected.length === 0 || selected.includes(actual)
}

function matchesAny(selected: string[], actual: string[]): boolean {
  return selected.length === 0 || selected.some((value) => actual.includes(value))
}
