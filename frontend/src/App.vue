<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRegisterSW } from 'virtual:pwa-register/vue'
import { useArticleStore } from './stores/articles'
import { usePersonalizationStore } from './stores/personalization'
import { getTheme } from './services/localDatabase'

const articles = useArticleStore()
const personalization = usePersonalizationStore()
const { offlineReady, needRefresh, updateServiceWorker } = useRegisterSW()
const navigation = [
  { to: '/', label: '今日', icon: '⌂' }, { to: '/categories', label: '分类', icon: '▦' },
  { to: '/following', label: '关注', icon: '◎' }, { to: '/favorites', label: '收藏', icon: '☆' },
  { to: '/settings', label: '设置', icon: '⚙' },
]

onMounted(async () => {
  const savedTheme = await getTheme()
  if (savedTheme && savedTheme !== 'system') document.documentElement.dataset.theme = savedTheme
  await Promise.all([articles.load(), personalization.load()])
})
</script>

<template>
  <div class="app-shell">
    <div v-if="offlineReady || needRefresh" class="update-banner" role="status">
      <span>{{ offlineReady ? '应用已可离线使用' : '发现新版本，准备好后刷新即可' }}</span>
      <button v-if="needRefresh" type="button" @click="updateServiceWorker(true)">刷新</button>
    </div>
    <main id="main-content" class="page-shell"><RouterView /></main>
    <nav class="bottom-nav" aria-label="主要导航">
      <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
        <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span><span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>
