<script setup lang="ts">
import { computed, ref } from 'vue'
import ArticleCard from '../components/ArticleCard.vue'
import DataState from '../components/DataState.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { matchesFollow } from '../services/articleSearch'
import { useArticleStore } from '../stores/articles'
import { usePersonalizationStore } from '../stores/personalization'
import type { FollowRecord } from '../services/localDatabase'

const articleStore = useArticleStore()
const personalization = usePersonalizationStore()
const keyword = ref('')
const kind = ref<FollowRecord['kind']>('keyword')
const matches = computed(() => articleStore.articles.filter((article) =>
  personalization.follows.some((follow) => matchesFollow(article, follow.keyword)),
))
const kindLabels: Record<FollowRecord['kind'], string> = { company: '企业', drug: '药品', disease: '疾病', keyword: '自由关键词' }

async function submit(): Promise<void> {
  await personalization.createFollow(kind.value, keyword.value)
  keyword.value = ''
}
</script>

<template>
  <PageHeader eyebrow="WATCHLIST" title="我的关注" description="关注信息只保存在这台设备，不上传账号或云端。" />
  <form class="follow-form" @submit.prevent="submit">
    <label>关注类型<select v-model="kind"><option v-for="(label, value) in kindLabels" :key="value" :value="value">{{ label }}</option></select></label>
    <label>名称或关键词<input v-model="keyword" maxlength="80" placeholder="例如：GLP-1、肺癌、某家公司" /></label>
    <button class="primary-button" type="submit" :disabled="!keyword.trim()">添加关注</button>
  </form>
  <section v-if="personalization.follows.length" class="watch-list" aria-label="已关注条目">
    <div v-for="follow in personalization.follows" :key="follow.id" class="watch-item">
      <div><span>{{ kindLabels[follow.kind] }}</span><strong>{{ follow.keyword }}</strong><small>{{ articleStore.articles.filter((article) => matchesFollow(article, follow.keyword)).length }} 条命中</small></div>
      <button type="button" :aria-label="`删除关注 ${follow.keyword}`" @click="personalization.deleteFollow(follow.id)">×</button>
    </div>
  </section>
  <DataState />
  <section v-if="articleStore.status === 'ready'" class="content-section">
    <div class="section-heading"><h2>最近命中</h2><span>{{ matches.length }} 条</span></div>
    <div v-if="matches.length" class="card-list"><ArticleCard v-for="article in matches" :key="article.id" :article="article" /></div>
    <EmptyState v-else title="还没有关注命中" description="添加企业、药品、疾病或关键词后，相关动态会汇总在这里。" />
  </section>
</template>
