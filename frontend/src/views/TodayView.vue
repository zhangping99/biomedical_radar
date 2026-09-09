<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import ArticleCard from '../components/ArticleCard.vue'
import DataState from '../components/DataState.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { searchArticles } from '../services/articleSearch'
import { diverseSelection, focusTopics, matchesFocus, type FocusTopic } from '../services/editorialFocus'
import { useArticleStore } from '../stores/articles'

const store = useArticleStore()
const query = ref('')
const debouncedQuery = ref('')
const visibleCount = ref(20)
const activeFocus = ref<FocusTopic>('academic')
let searchTimer: ReturnType<typeof setTimeout> | undefined

watch(query, (value) => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { debouncedQuery.value = value.trim() }, 220)
})
onBeforeUnmount(() => clearTimeout(searchTimer))

const searchResults = computed(() => debouncedQuery.value ? searchArticles(store.articles, debouncedQuery.value) : [])
const visibleLatest = computed(() => store.sorted.slice(0, visibleCount.value))
const focused = computed(() => store.sorted.filter(article => matchesFocus(article, activeFocus.value)))
const focusCount = ref(6)
const focusPreview = computed(() => diverseSelection(focused.value, focusCount.value, Math.max(2, Math.ceil(focusCount.value / 3))))
watch(activeFocus, () => { focusCount.value = 6 })
const dateLabel = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
</script>

<template>
  <PageHeader eyebrow="PERSONAL INTELLIGENCE" title="今日医药雷达" :description="`${dateLabel} · 用五分钟抓住关键变化`">
    <label class="search-box">
      <span aria-hidden="true">⌕</span><span class="sr-only">搜索文章</span>
      <input v-model="query" type="search" autocomplete="off" placeholder="搜索公司、药品、疾病或关键词" />
      <button v-if="query" type="button" aria-label="清空搜索" @click="query = ''">×</button>
    </label>
  </PageHeader>
  <DataState />

  <template v-if="store.status === 'ready'">
    <section v-if="debouncedQuery" class="content-section" aria-live="polite">
      <div class="section-heading"><div><p class="eyebrow">SEARCH</p><h2>搜索结果</h2></div><span>{{ searchResults.length }} 条</span></div>
      <p class="result-hint">在标题、摘要、实体、疾病和事件字段中匹配“{{ debouncedQuery }}”</p>
      <div v-if="searchResults.length" class="card-list"><ArticleCard v-for="article in searchResults" :key="article.id" :article="article" /></div>
      <EmptyState v-else title="没有找到匹配内容" description="换一个更短的公司名、药品名或疾病关键词试试。" />
    </section>

    <template v-else>
      <section class="daily-brief">
        <p class="eyebrow">DAILY BRIEF</p>
        <p>本期共收录 <strong>{{ store.articles.length }}</strong> 条，筛出 <strong>{{ store.mustRead.length }}</strong> 条今日必读。</p>
      </section>
      <section class="content-section" aria-label="重点关注">
        <div class="section-heading"><div><p class="eyebrow">FOCUS</p><h2>重点关注</h2></div><span>{{ focused.length }} 条</span></div>
        <div class="chip-row">
          <button v-for="topic in focusTopics" :key="topic.id" type="button" class="chip" :class="{ active: activeFocus === topic.id }" :aria-pressed="activeFocus === topic.id" @click="activeFocus = topic.id">{{ topic.label }}</button>
        </div>
        <p class="result-hint">按发布时间精选，同一来源限量展示；完整动态仍保留在下方最新列表。</p>
        <div v-if="focusPreview.length" class="card-list"><ArticleCard v-for="article in focusPreview" :key="article.id" :article="article" /></div>
        <EmptyState v-else title="本期暂无此类动态" description="等待下一次采集；不以旧闻或无关内容补足数量。" />
        <button v-if="focusPreview.length < focused.length" type="button" class="secondary-button full-width" @click="focusCount += 6">查看更多此类动态</button>
      </section>
      <section class="content-section">
        <div class="section-heading"><div><p class="eyebrow">MUST READ</p><h2>今日必读</h2></div><span>按影响力排序</span></div>
        <div v-if="store.mustRead.length" class="card-list"><ArticleCard v-for="article in store.mustRead" :key="article.id" :article="article" /></div>
        <EmptyState v-else title="今天暂无必读" description="C 级线索不会进入必读；新一批数据到达后会自动更新。" />
      </section>
      <section v-if="store.highImpact.length" class="content-section">
        <div class="section-heading"><div><p class="eyebrow">HIGH IMPACT</p><h2>政策与安全</h2></div></div>
        <div class="compact-grid"><ArticleCard v-for="article in store.highImpact" :key="article.id" :article="article" compact /></div>
      </section>
      <section class="content-section">
        <div class="section-heading"><div><p class="eyebrow">LATEST</p><h2>最新动态</h2></div><span>{{ store.sorted.length }} 条</span></div>
        <div class="card-list"><ArticleCard v-for="article in visibleLatest" :key="article.id" :article="article" /></div>
        <button v-if="visibleCount < store.sorted.length" class="secondary-button full-width" type="button" @click="visibleCount += 20">加载更多日期</button>
      </section>
    </template>
  </template>
</template>
