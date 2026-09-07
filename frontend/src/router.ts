import { createRouter, createWebHistory } from 'vue-router'
import TodayView from './views/TodayView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash }
    if (to.name === from.name) return false
    return { top: 0 }
  },
  routes: [
    { path: '/', name: 'today', component: TodayView },
    { path: '/categories', name: 'categories', component: () => import('./views/CategoryView.vue') },
    { path: '/articles/:id', name: 'article', component: () => import('./views/ArticleDetailView.vue') },
    { path: '/following', name: 'following', component: () => import('./views/FollowView.vue') },
    { path: '/favorites', name: 'favorites', component: () => import('./views/FavoritesView.vue') },
    { path: '/settings', name: 'settings', component: () => import('./views/SettingsView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? routeTitle(to.name))} · 医药雷达`
})

function routeTitle(name: unknown): string {
  return ({ today: '今日', categories: '分类', article: '详情', following: '关注', favorites: '收藏', settings: '设置' } as Record<string, string>)[String(name)] ?? '医药雷达'
}

export default router
