import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.goto('/biomedical_radar/')
})

test('loads the static daily briefing and opens a traceable detail', async ({ page }) => {
  await expect(page.getByRole('heading', { name: '今日医药雷达' })).toBeVisible()
  await expect(page.getByText(/本期共收录/)).toBeVisible()
  const firstArticle = page.locator('.article-card').first()
  await expect(firstArticle).toBeVisible()
  await firstArticle.getByRole('link').first().click()
  await expect(page.getByRole('heading', { name: '事实摘要' })).toBeVisible()
  await expect(page.getByRole('link', { name: /查看原始来源/ })).toHaveAttribute('href', /^https:\/\//)
})

test('supports filtering and local favorites', async ({ page }) => {
  await page.getByRole('link', { name: /分类/ }).click()
  await page.getByRole('button', { name: '美国' }).click()
  await expect(page.getByText(/筛选结果/)).toBeVisible()
  await page.getByRole('link', { name: /今日/ }).click()
  await page.locator('.article-card').first().getByRole('button', { name: '收藏文章' }).click()
  await page.getByRole('link', { name: /收藏/ }).click()
  await expect(page.locator('.article-card')).toHaveCount(1)
})

test('renders at mobile width without horizontal overflow', async ({ page }) => {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  expect(overflow).toBe(false)
  await expect(page.getByRole('navigation', { name: '主要导航' })).toBeVisible()
})

test('falls back to the last IndexedDB feed when all data requests fail', async ({ page }) => {
  await expect(page.locator('.article-card').first()).toBeVisible()
  await page.evaluate(async () => {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(registrations.map((registration) => registration.unregister()))
  })
  await page.route('**/data/**', (route) => route.abort('failed'))
  await page.reload()
  await expect(page.getByText('当前为离线缓存')).toBeVisible()
  await expect(page.locator('.article-card').first()).toBeVisible()
})
