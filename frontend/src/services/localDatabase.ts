import { openDB, type DBSchema } from 'idb'
import type { Article, FeedManifest, SourceHealthDocument } from '../domain/article'

export interface ArticleSnapshot {
  id: string
  title: string
  sourceName: string
  publishedAt: string | null
  originalUrl: string
}

export interface FavoriteRecord {
  id: string
  savedAt: string
  snapshot: ArticleSnapshot
}

export interface FollowRecord {
  id: string
  kind: 'company' | 'drug' | 'disease' | 'keyword'
  keyword: string
  createdAt: string
}

export interface CachedFeed {
  manifest: FeedManifest
  articles: Article[]
  sourceHealth: SourceHealthDocument | null
}

interface RadarDatabase extends DBSchema {
  favorites: { key: string; value: FavoriteRecord }
  follows: { key: string; value: FollowRecord }
  reads: { key: string; value: { id: string; readAt: string } }
  cache: { key: string; value: { key: string; value: CachedFeed } }
  settings: { key: string; value: { key: string; value: string } }
}

const DATABASE_NAME = 'biomedical-radar'
export const DATABASE_VERSION = 1

export const database = openDB<RadarDatabase>(DATABASE_NAME, DATABASE_VERSION, {
  upgrade(db, oldVersion) {
    if (oldVersion < 1) {
      db.createObjectStore('favorites', { keyPath: 'id' })
      db.createObjectStore('follows', { keyPath: 'id' })
      db.createObjectStore('reads', { keyPath: 'id' })
      db.createObjectStore('cache', { keyPath: 'key' })
      db.createObjectStore('settings', { keyPath: 'key' })
    }
  },
})

export function toSnapshot(article: Article): ArticleSnapshot {
  return {
    id: article.id,
    title: article.titleZh || article.titleOriginal,
    sourceName: article.sourceName,
    publishedAt: article.publishedAt,
    originalUrl: article.originalUrl,
  }
}

export async function listFavorites(): Promise<FavoriteRecord[]> {
  return (await (await database).getAll('favorites')).sort((a, b) => b.savedAt.localeCompare(a.savedAt))
}

export async function setFavorite(article: Article, favorite: boolean): Promise<void> {
  const db = await database
  if (favorite) {
    await db.put('favorites', { id: article.id, savedAt: new Date().toISOString(), snapshot: toSnapshot(article) })
  } else {
    await db.delete('favorites', article.id)
  }
}

export async function listFollows(): Promise<FollowRecord[]> {
  return (await (await database).getAll('follows')).sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

export async function addFollow(kind: FollowRecord['kind'], keyword: string): Promise<FollowRecord> {
  const clean = keyword.trim()
  const record: FollowRecord = {
    id: `${kind}:${clean.toLocaleLowerCase()}`,
    kind,
    keyword: clean,
    createdAt: new Date().toISOString(),
  }
  await (await database).put('follows', record)
  return record
}

export async function removeFollow(id: string): Promise<void> {
  await (await database).delete('follows', id)
}

export async function listReadIds(): Promise<string[]> {
  return (await (await database).getAllKeys('reads')).map(String)
}

export async function markRead(id: string, read: boolean): Promise<void> {
  const db = await database
  if (read) await db.put('reads', { id, readAt: new Date().toISOString() })
  else await db.delete('reads', id)
}

export async function saveCachedFeed(value: CachedFeed): Promise<void> {
  await (await database).put('cache', { key: 'latest-feed', value })
}

export async function loadCachedFeed(): Promise<CachedFeed | null> {
  return (await (await database).get('cache', 'latest-feed'))?.value ?? null
}

export async function clearFeedCache(): Promise<void> {
  await (await database).clear('cache')
  const registrations = await navigator.serviceWorker?.getRegistrations?.()
  await Promise.all((registrations ?? []).map((registration) => registration.update()))
  const names = await globalThis.caches?.keys?.()
  await Promise.all((names ?? []).filter((name) => name.includes('data')).map((name) => caches.delete(name)))
}

export interface UserBackupV1 {
  backupVersion: 1
  exportedAt: string
  favorites: FavoriteRecord[]
  follows: FollowRecord[]
  readIds: string[]
}

export async function exportUserData(): Promise<UserBackupV1> {
  return {
    backupVersion: 1,
    exportedAt: new Date().toISOString(),
    favorites: await listFavorites(),
    follows: await listFollows(),
    readIds: await listReadIds(),
  }
}

export function parseBackup(raw: string): UserBackupV1 {
  const value = JSON.parse(raw) as Partial<UserBackupV1>
  if (value.backupVersion !== 1 || !Array.isArray(value.favorites) || !Array.isArray(value.follows)) {
    throw new Error('只支持 backupVersion=1 的医药雷达备份')
  }
  return value as UserBackupV1
}

export async function importUserData(value: UserBackupV1): Promise<void> {
  const db = await database
  const tx = db.transaction(['favorites', 'follows', 'reads'], 'readwrite')
  await Promise.all([
    ...value.favorites.map((favorite) => tx.objectStore('favorites').put(favorite)),
    ...value.follows.map((follow) => tx.objectStore('follows').put(follow)),
    ...(value.readIds ?? []).map((id) => tx.objectStore('reads').put({ id, readAt: value.exportedAt })),
    tx.done,
  ])
}

export async function clearUserData(): Promise<void> {
  const db = await database
  const tx = db.transaction(['favorites', 'follows', 'reads'], 'readwrite')
  await Promise.all([
    tx.objectStore('favorites').clear(),
    tx.objectStore('follows').clear(),
    tx.objectStore('reads').clear(),
    tx.done,
  ])
}

export async function getTheme(): Promise<string | null> {
  return (await (await database).get('settings', 'theme'))?.value ?? null
}

export async function setTheme(value: string): Promise<void> {
  await (await database).put('settings', { key: 'theme', value })
}
