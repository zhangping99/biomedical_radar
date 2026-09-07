<script setup lang="ts">
import { useArticleStore } from '../stores/articles'
const store = useArticleStore()
</script>

<template>
  <div v-if="store.status === 'loading'" class="state-card" role="status" aria-live="polite">
    <span class="spinner" aria-hidden="true" />
    <div><strong>正在读取最新雷达数据</strong><p>弱网时最多等待几秒，随后会尝试本地缓存。</p></div>
  </div>
  <div v-else-if="store.status === 'error'" class="state-card state-error" role="alert">
    <div><strong>暂时无法加载数据</strong><p>{{ store.error }}</p></div>
    <button type="button" @click="store.load(true)">重试</button>
  </div>
  <div v-else-if="store.fromCache || store.isStale || store.manifest?.freshness === 'partial'" class="notice" role="status">
    <strong>{{ store.fromCache ? '当前为离线缓存' : store.isStale ? '数据可能已过期' : '部分来源采集失败' }}</strong>
    <span>最近生成：{{ store.manifest ? new Date(store.manifest.generatedAt).toLocaleString('zh-CN') : '未知' }}</span>
  </div>
</template>
