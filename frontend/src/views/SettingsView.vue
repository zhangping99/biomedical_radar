<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import DataState from '../components/DataState.vue'
import PageHeader from '../components/PageHeader.vue'
import {
  clearFeedCache, clearUserData, exportUserData, importUserData, parseBackup, setTheme,
  type UserBackupV1,
} from '../services/localDatabase'
import { useArticleStore } from '../stores/articles'
import { usePersonalizationStore } from '../stores/personalization'

const articles = useArticleStore()
const personalization = usePersonalizationStore()
const theme = ref(document.documentElement.dataset.theme ?? 'system')
const storageLabel = ref('正在计算…')
const message = ref('')
const pendingImport = ref<UserBackupV1 | null>(null)
const appVersion = __APP_VERSION__
const buildTime = __BUILD_TIME__
const commitSha = __COMMIT_SHA__
const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent)
const isAndroid = /Android/.test(navigator.userAgent)
const failedSources = computed(() => articles.sourceHealth?.sources.filter((source) => source.status === 'failed') ?? [])

onMounted(async () => {
  const estimate = await navigator.storage?.estimate?.()
  storageLabel.value = estimate?.usage == null ? '浏览器未提供' : `${(estimate.usage / 1024 / 1024).toFixed(1)} MB`
})

async function changeTheme(): Promise<void> {
  if (theme.value === 'system') delete document.documentElement.dataset.theme
  else document.documentElement.dataset.theme = theme.value
  await setTheme(theme.value)
}

async function downloadBackup(): Promise<void> {
  const content = JSON.stringify(await exportUserData(), null, 2)
  const url = URL.createObjectURL(new Blob([content], { type: 'application/json' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `biomedical-radar-backup-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(url)
  message.value = '备份已导出'
}

async function chooseBackup(event: Event): Promise<void> {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    pendingImport.value = parseBackup(await file.text())
    message.value = `待导入：${pendingImport.value.favorites.length} 条收藏、${pendingImport.value.follows.length} 条关注；重复 ID 会合并。`
  } catch (error) {
    pendingImport.value = null
    message.value = error instanceof Error ? error.message : '备份文件无法读取'
  }
}

async function confirmImport(): Promise<void> {
  if (!pendingImport.value) return
  await importUserData(pendingImport.value)
  await personalization.load()
  pendingImport.value = null
  message.value = '导入完成，重复记录已合并'
}

async function clearCache(): Promise<void> {
  if (!window.confirm('清除离线新闻缓存？收藏和关注不会删除。')) return
  await clearFeedCache()
  message.value = '离线缓存已清除，下次打开会重新下载'
}

async function clearPersonalData(): Promise<void> {
  if (!window.confirm('永久清除本机收藏、关注和已读状态？建议先导出备份。')) return
  await clearUserData()
  await personalization.load()
  message.value = '本机个人数据已清除'
}
</script>

<template>
  <PageHeader eyebrow="PREFERENCES" title="设置" description="应用、缓存和本机数据控制。" />
  <DataState />
  <div class="settings-stack">
    <section class="settings-card">
      <h2>安装到主屏幕</h2>
      <ol v-if="isIos"><li>在 Safari 中打开本站。</li><li>点击“分享”，选择“添加到主屏幕”。</li><li>从主屏幕启动，确认独立窗口显示。</li></ol>
      <ol v-else-if="isAndroid"><li>在 Chrome 中打开本站。</li><li>打开菜单，选择“安装应用”或“添加到主屏幕”。</li><li>从主屏幕启动。</li></ol>
      <p v-else>请使用浏览器菜单中的“安装应用”或“添加到主屏幕”。iPhone 需在 Safari 中操作。</p>
      <p class="muted">微信内置浏览器仅保证基础浏览，请转到系统浏览器安装。</p>
    </section>
    <section class="settings-card">
      <h2>显示</h2>
      <label class="setting-row">主题<select v-model="theme" @change="changeTheme"><option value="system">跟随系统</option><option value="light">浅色</option><option value="dark">深色</option></select></label>
    </section>
    <section class="settings-card">
      <h2>数据与来源</h2>
      <dl class="settings-facts"><div><dt>数据生成时间</dt><dd>{{ articles.manifest ? new Date(articles.manifest.generatedAt).toLocaleString('zh-CN') : '尚未加载' }}</dd></div><div><dt>来源状态</dt><dd>{{ articles.sourceHealth ? `${articles.sourceHealth.sources.length - failedSources.length}/${articles.sourceHealth.sources.length} 正常` : '未知' }}</dd></div><div><dt>浏览器存储</dt><dd>{{ storageLabel }}</dd></div></dl>
      <ul v-if="failedSources.length" class="source-failures"><li v-for="source in failedSources" :key="source.sourceId">{{ source.sourceName }}：{{ source.errorCode ?? '采集失败' }}</li></ul>
      <button class="secondary-button" type="button" @click="clearCache">清除离线缓存</button>
    </section>
    <section class="settings-card">
      <h2>收藏与关注备份</h2>
      <p>备份只包含收藏快照、关注词和已读 ID，不含账号或浏览历史。</p>
      <div class="button-row"><button class="secondary-button" type="button" @click="downloadBackup">导出 JSON</button><label class="secondary-button file-button">选择备份<input type="file" accept="application/json,.json" @change="chooseBackup" /></label></div>
      <button v-if="pendingImport" class="primary-button full-width" type="button" @click="confirmImport">确认合并导入</button>
      <button class="danger-button" type="button" @click="clearPersonalData">清除本机个人数据</button>
    </section>
    <section class="settings-card">
      <h2>内容说明</h2>
      <p>本站仅聚合公开来源的标题、元数据、短摘要和原文链接，不转载第三方全文。机器生成字段会明确标记，内容可能存在延迟或错误。</p>
      <p><strong>不构成医疗、诊断、用药或投资建议。</strong>请始终以监管机构、注册库、论文和企业原始公告为准。</p>
    </section>
    <section class="settings-card version-card">
      <h2>版本</h2><p>V{{ appVersion }} · {{ commitSha }}</p><small>构建于 {{ new Date(buildTime).toLocaleString('zh-CN') }}</small>
    </section>
  </div>
  <p v-if="message" class="action-message floating-message" role="status">{{ message }}</p>
</template>
