<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import { categoryLabels, displayTitle, eventLabels, regionLabels } from '../domain/article'
import { useArticleStore } from '../stores/articles'
import { usePersonalizationStore } from '../stores/personalization'

const route = useRoute()
const router = useRouter()
const store = useArticleStore()
const personalization = usePersonalizationStore()
const shareMessage = ref('')
const article = computed(() => store.find(String(route.params.id)))
const isFavorite = computed(() => article.value ? personalization.favoriteIds.has(article.value.id) : false)
const isRead = computed(() => article.value ? personalization.readIdSet.has(article.value.id) : false)

async function share(): Promise<void> {
  if (!article.value) return
  const data = { title: displayTitle(article.value), text: `${displayTitle(article.value)} · ${article.value.sourceName}`, url: article.value.canonicalUrl }
  try {
    if (navigator.share) await navigator.share(data)
    else {
      await navigator.clipboard.writeText(`${data.text}\n${data.url}`)
      shareMessage.value = '原文链接已复制'
    }
  } catch (error) {
    if ((error as DOMException).name !== 'AbortError') shareMessage.value = '分享失败，可直接打开原文复制链接'
  }
}
</script>

<template>
  <header class="detail-toolbar">
    <button type="button" aria-label="返回上一页" @click="router.back()">←</button><span>动态详情</span>
    <button type="button" @click="share">分享</button>
  </header>
  <article v-if="article" class="detail-article">
    <div class="article-meta"><span class="tier-badge">{{ article.sourceTier }}级</span><span>{{ regionLabels[article.region] }}</span><span>{{ categoryLabels[article.category] }}</span></div>
    <h1>{{ displayTitle(article) }}</h1>
    <p v-if="article.titleZh && article.titleZh !== article.titleOriginal" class="detail-original">{{ article.titleOriginal }}</p>
    <div class="tag-row"><span v-for="event in article.eventTypes" :key="event" class="tag">{{ eventLabels[event] ?? event }}</span><span v-if="article.generated" class="tag generated">机器生成</span></div>

    <section class="detail-block"><h2>事实摘要</h2><p>{{ article.summaryZh || '当前降级模式未生成中文摘要，请通过原始来源核验。' }}</p></section>
    <section class="detail-block"><h2>为什么值得关注</h2><p>{{ article.whyItMattersZh || article.importanceReasons.join(' · ') || '暂无自动判断。' }}</p><p class="disclaimer">仅用于行业信息整理，不构成医疗、用药或投资建议。</p></section>
    <section v-if="article.entities.length || article.diseaseAreas.length" class="detail-block"><h2>相关对象</h2><div class="fact-list"><span v-for="entity in article.entities" :key="`${entity.type}:${entity.name}`">{{ entity.name }}</span><span v-for="disease in article.diseaseAreas" :key="disease">{{ disease }}</span></div></section>
    <section class="source-card">
      <p class="eyebrow">SOURCE TRACE</p><h2>{{ article.sourceName }}</h2>
      <dl><div><dt>来源级别</dt><dd>{{ article.sourceTier }} 级</dd></div><div><dt>原始发布时间</dt><dd>{{ article.publishedAt ? new Date(article.publishedAt).toLocaleString('zh-CN') : '来源未提供' }}</dd></div><div><dt>采集时间</dt><dd>{{ new Date(article.collectedAt).toLocaleString('zh-CN') }}</dd></div><div><dt>核验状态</dt><dd>{{ article.verificationStatus }}</dd></div></dl>
      <p v-if="article.originalAccessStatus !== 'reachable'" class="notice">原文当前可能访问缓慢或不可用。</p>
      <a class="primary-button full-width" :href="article.canonicalUrl" target="_blank" rel="noopener noreferrer">查看原始来源 ↗</a>
    </section>
    <div class="detail-actions">
      <button type="button" @click="personalization.toggleFavorite(article)">{{ isFavorite ? '★ 已收藏' : '☆ 收藏' }}</button>
      <button type="button" @click="personalization.toggleRead(article.id)">{{ isRead ? '标为未读' : '标为已读' }}</button>
    </div>
    <p v-if="shareMessage" class="action-message" role="status">{{ shareMessage }}</p>
  </article>
  <EmptyState v-else-if="store.status === 'ready'" title="这条动态已不在当前数据中" description="最近 90 天数据可能已滚动更新；收藏中的最小快照仍会保留。"><button type="button" class="secondary-button" @click="router.push('/')">返回今日</button></EmptyState>
</template>
