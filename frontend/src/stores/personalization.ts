import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Article } from '../domain/article'
import {
  addFollow, listFavorites, listFollows, listReadIds, markRead, removeFollow, setFavorite,
  type FavoriteRecord, type FollowRecord,
} from '../services/localDatabase'

export const usePersonalizationStore = defineStore('personalization', () => {
  const favorites = ref<FavoriteRecord[]>([])
  const follows = ref<FollowRecord[]>([])
  const readIds = ref<string[]>([])
  const loaded = ref(false)

  const favoriteIds = computed(() => new Set(favorites.value.map((item) => item.id)))
  const readIdSet = computed(() => new Set(readIds.value))

  async function load(): Promise<void> {
    ;[favorites.value, follows.value, readIds.value] = await Promise.all([
      listFavorites(), listFollows(), listReadIds(),
    ])
    loaded.value = true
  }

  async function toggleFavorite(article: Article): Promise<void> {
    const next = !favoriteIds.value.has(article.id)
    await setFavorite(article, next)
    favorites.value = await listFavorites()
  }

  async function toggleRead(id: string): Promise<void> {
    await markRead(id, !readIdSet.value.has(id))
    readIds.value = await listReadIds()
  }

  async function createFollow(kind: FollowRecord['kind'], keyword: string): Promise<void> {
    if (!keyword.trim()) return
    await addFollow(kind, keyword)
    follows.value = await listFollows()
  }

  async function deleteFollow(id: string): Promise<void> {
    await removeFollow(id)
    follows.value = await listFollows()
  }

  return { favorites, follows, readIds, loaded, favoriteIds, readIdSet, load, toggleFavorite, toggleRead, createFollow, deleteFollow }
})
