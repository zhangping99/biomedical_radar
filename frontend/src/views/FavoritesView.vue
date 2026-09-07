<script setup lang="ts">
import { computed } from 'vue'
import ArticleCard from '../components/ArticleCard.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { useArticleStore } from '../stores/articles'
import { usePersonalizationStore } from '../stores/personalization'

const articles = useArticleStore()
const personalization = usePersonalizationStore()
const available = computed(() => personalization.favorites.flatMap((favorite) => {
  const article = articles.find(favorite.id)
  return article ? [article] : []
}))
const archived = computed(() => personalization.favorites.filter((favorite) => !articles.find(favorite.id)))
</script>

<template>
  <PageHeader eyebrow="SAVED" title="收藏与稍后读" description="按收藏时间倒序；文章滚出 90 天窗口后仍保留最小快照。" />
  <section class="content-section">
    <div class="section-heading"><h2>我的收藏</h2><span>{{ personalization.favorites.length }} 条</span></div>
    <div v-if="available.length" class="card-list"><ArticleCard v-for="article in available" :key="article.id" :article="article" /></div>
    <EmptyState v-else-if="!archived.length" title="还没有收藏" description="在动态卡片或详情页点击星标，稍后从这里继续阅读。" />
    <div v-if="archived.length" class="archived-list">
      <h2>历史快照</h2>
      <a v-for="favorite in archived" :key="favorite.id" :href="favorite.snapshot.originalUrl" target="_blank" rel="noopener noreferrer" class="snapshot-card"><strong>{{ favorite.snapshot.title }}</strong><span>{{ favorite.snapshot.sourceName }} · {{ favorite.snapshot.publishedAt ? new Date(favorite.snapshot.publishedAt).toLocaleDateString('zh-CN') : '时间未知' }}</span></a>
    </div>
  </section>
</template>
