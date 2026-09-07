import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'iPhone Safari', use: { ...devices['iPhone 13'] } },
    { name: 'Android Chrome', use: { ...devices['Pixel 7'] } },
  ],
  webServer: {
    command: 'pnpm build && pnpm preview --host 127.0.0.1',
    url: 'http://127.0.0.1:4173/biomedical_radar/',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
