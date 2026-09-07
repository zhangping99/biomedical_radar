<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { categoryLabels, displayTitle, eventLabels, regionLabels, type Article } from '../domain/article'
import { usePersonalizationStore } from '../stores/personalization'

const props = defineProps<{ article: Article; compact?: boolean }>()
const personalization = usePersonalizationStore()
const isFavorite = computed(() => personalization.favoriteIds.has(props.article.id))
const isRead = computed(() => personalization.readIdSet.has(props.article.id))
const date = computed(() => new Date(props.article.publishedAt ?? props.article.collectedAt).toLocaleDateString('zh-CN', {
  month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
}))

function openArticle(): void {
  void personalization.toggleRead(props.article.id)
}
</script>

<template>
  <article class="article-card" :class="{ compact, read: isRead }">
    <div class="article-meta">
      <span class="tier-badge">{{ article.sourceTier }}级</span><span>{{ regionLabels[article.region] }}</span>
      <span>{{ categoryLabels[article.category] }}</span>
      <time :datetime="article.publishedAt ?? article.collectedAt">{{ date }}</time>
    </div>
    <RouterLink class="article-link" :to="`/articles/${article.id}`" @click="openArticle">
      <h3>{{ displayTitle(article) }}</h3>
      <p v-if="article.titleZh && article.titleZh !== article.titleOriginal" class="original-title">{{ article.titleOriginal }}</p>
      <p v-if="!compact" class="article-summary">{{ article.summaryZh || '暂无中文摘要，可打开详情核验来源并查看原文。' }}</p>
    </RouterLink>
    <div class="article-footer">
      <div class="tag-row">
        <span v-for="event in article.eventTypes.slice(0, 3)" :key="event" class="tag">{{ eventLabels[event] ?? event }}</span>
        <span v-if="article.generated" class="tag generated">机器生成</span>
      </div>
      <button class="icon-button" type="button" :aria-label="isFavorite ? '取消收藏' : '收藏文章'"
              :aria-pressed="isFavorite" @click="personalization.toggleFavorite(article)">{{ isFavorite ? '★' : '☆' }}</button>
    </div>
    <div v-if="article.importanceReasons.length && !compact" class="reason-row">
      <strong>入选理由</strong> {{ article.importanceReasons.slice(0, 2).join(' · ') }}
    </div>
  </article>
</template>
