import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Article, FeedManifest, SourceHealthDocument } from '../domain/article'
import { articleTimestamp } from '../domain/article'
import { StaticArticleRepository } from '../repositories/staticArticleRepository'
import { diverseSelection } from '../services/editorialFocus'

export const useArticleStore = defineStore('articles', () => {
  const articles = ref<Article[]>([])
  const manifest = ref<FeedManifest | null>(null)
  const sourceHealth = ref<SourceHealthDocument | null>(null)
  const status = ref<'idle' | 'loading' | 'ready' | 'error'>('idle')
  const error = ref('')
  const fromCache = ref(false)
  const repository = new StaticArticleRepository()

  const sorted = computed(() => [...articles.value].sort((a, b) => articleTimestamp(b) - articleTimestamp(a)))
  const mustRead = computed(() => diverseSelection(articles.value
    .filter((article) => article.sourceTier !== 'C' && article.importanceScore >= 60)
    .sort((a, b) => b.importanceScore - a.importanceScore || articleTimestamp(b) - articleTimestamp(a)), 10))
  const highImpact = computed(() => articles.value
    .filter((article) => ['policy_regulation', 'quality_safety'].includes(article.category))
    .sort((a, b) => b.importanceScore - a.importanceScore)
    .slice(0, 6))
  const isStale = computed(() => {
    if (!manifest.value) return false
    return Date.now() - Date.parse(manifest.value.generatedAt) > 12 * 60 * 60 * 1000
  })

  async function load(force = false): Promise<void> {
    if (!force && (status.value === 'loading' || status.value === 'ready')) return
    status.value = 'loading'
    error.value = ''
    try {
      const result = await repository.loadLatest()
      articles.value = result.articles
      manifest.value = result.manifest
      sourceHealth.value = result.sourceHealth
      fromCache.value = result.fromCache
      status.value = 'ready'
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '数据加载失败，请稍后重试。'
      status.value = 'error'
    }
  }

  function find(id: string): Article | undefined {
    return articles.value.find((article) => article.id === id)
  }

  return { articles, manifest, sourceHealth, status, error, fromCache, sorted, mustRead, highImpact, isStale, load, find }
})
