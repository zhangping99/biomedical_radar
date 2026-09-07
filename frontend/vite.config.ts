/// <reference types="vitest/config" />
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => ({
  base: mode === 'production' ? '/biomedical_radar/' : '/',
  define: {
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version ?? '1.0.0'),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
    __COMMIT_SHA__: JSON.stringify(process.env.GITHUB_SHA?.slice(0, 7) ?? 'local'),
  },
  plugins: [
    vue(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['favicon.svg', 'icons/icon-192.png', 'icons/icon-512.png'],
      manifest: {
        name: '医药雷达',
        short_name: '医药雷达',
        description: '个人使用的医药行业静态信息雷达',
        theme_color: '#113f36',
        background_color: '#f4f7f3',
        display: 'standalone',
        start_url: '.',
        scope: '.',
        lang: 'zh-CN',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        navigateFallbackDenylist: [/^\/data\//],
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.includes('/data/'),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'biomedical-radar-data-v1',
              networkTimeoutSeconds: 4,
              expiration: { maxEntries: 120, maxAgeSeconds: 60 * 60 * 24 * 90 },
            },
          },
        ],
      },
      devOptions: { enabled: true },
    }),
  ],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    coverage: { reporter: ['text', 'html'] },
  },
}))
