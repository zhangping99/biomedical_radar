<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ArticleCard from '../components/ArticleCard.vue'
import DataState from '../components/DataState.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { categories, categoryLabels, eventLabels, regionLabels, type Category, type Region, type SourceTier } from '../domain/article'
import { emptyFilters, filterArticles } from '../services/articleSearch'
import { useArticleStore } from '../stores/articles'

const store = useArticleStore()
const route = useRoute()
const router = useRouter()
const filters = reactive(emptyFilters())
const regionOptions: Region[] = ['CN', 'US', 'EU', 'JP', 'GLOBAL', 'OTHER']
const tierOptions: SourceTier[] = ['A', 'B', 'C']
const eventOptions = computed(() => [...new Set(store.articles.flatMap((article) => article.eventTypes))].sort())
const diseaseOptions = computed(() => [...new Set(store.articles.flatMap((article) => article.diseaseAreas))].sort())
const results = computed(() => filterArticles([...store.articles], filters))
const hasFilters = computed(() => filters.regions.length + filters.categories.length + filters.events.length + filters.diseases.length + filters.sourceTiers.length > 0)

function values(key: string): string[] {
  const raw = route.query[key]
  return typeof raw === 'string' && raw ? raw.split(',') : []
}
filters.regions = values('region') as Region[]
filters.categories = values('category') as Category[]
filters.events = values('event')
filters.diseases = values('disease')
filters.sourceTiers = values('tier') as SourceTier[]
filters.sort = route.query.sort === 'importance' ? 'importance' : 'newest'

watch(filters, () => {
  const query: Record<string, string> = {}
  if (filters.regions.length) query.region = filters.regions.join(',')
  if (filters.categories.length) query.category = filters.categories.join(',')
  if (filters.events.length) query.event = filters.events.join(',')
  if (filters.diseases.length) query.disease = filters.diseases.join(',')
  if (filters.sourceTiers.length) query.tier = filters.sourceTiers.join(',')
  if (filters.sort !== 'newest') query.sort = filters.sort
  void router.replace({ query })
}, { deep: true })

function toggle<T>(values: T[], value: T): void {
  const index = values.indexOf(value)
  if (index >= 0) values.splice(index, 1)
  else values.push(value)
}

function clear(): void {
  Object.assign(filters, emptyFilters())
}
</script>

<template>
  <PageHeader eyebrow="EXPLORE" title="分类筛选" description="同组取其一，不同组同时满足；筛选状态可通过链接保留。" />
  <DataState />
  <template v-if="store.status === 'ready'">
    <section class="filter-panel">
      <div class="filter-title"><h2>地区</h2><button v-if="hasFilters" type="button" @click="clear">清空全部</button></div>
      <div class="chip-row"><button v-for="region in regionOptions" :key="region" class="chip" :class="{ active: filters.regions.includes(region) }" type="button" :aria-pressed="filters.regions.includes(region)" @click="toggle(filters.regions, region)">{{ regionLabels[region] }}</button></div>
      <h2>一级栏目</h2>
      <div class="chip-row"><button v-for="category in categories" :key="category" class="chip" :class="{ active: filters.categories.includes(category) }" type="button" :aria-pressed="filters.categories.includes(category)" @click="toggle(filters.categories, category)">{{ categoryLabels[category] }}</button></div>
      <details>
        <summary>更多条件</summary>
        <h2>事件</h2>
        <div class="chip-row"><button v-for="event in eventOptions" :key="event" class="chip" :class="{ active: filters.events.includes(event) }" type="button" @click="toggle(filters.events, event)">{{ eventLabels[event] ?? event }}</button></div>
        <h2 v-if="diseaseOptions.length">疾病领域</h2>
        <div class="chip-row"><button v-for="disease in diseaseOptions" :key="disease" class="chip" :class="{ active: filters.diseases.includes(disease) }" type="button" @click="toggle(filters.diseases, disease)">{{ disease }}</button></div>
        <h2>来源级别</h2>
        <div class="chip-row"><button v-for="tier in tierOptions" :key="tier" class="chip" :class="{ active: filters.sourceTiers.includes(tier) }" type="button" @click="toggle(filters.sourceTiers, tier)">{{ tier }} 级</button></div>
      </details>
      <label class="sort-control">排序<select v-model="filters.sort"><option value="newest">最新发布</option><option value="importance">重要度优先</option></select></label>
    </section>
    <section class="content-section">
      <div class="section-heading"><h2>筛选结果</h2><span>{{ results.length }} 条</span></div>
      <div v-if="results.length" class="card-list"><ArticleCard v-for="article in results" :key="article.id" :article="article" /></div>
      <EmptyState v-else title="没有符合条件的动态" description="可以移除一个筛选条件，或等待下一次采集。" />
    </section>
  </template>
</template>
