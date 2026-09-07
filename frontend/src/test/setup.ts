import 'fake-indexeddb/auto'
import { afterEach } from 'vitest'

afterEach(() => {
  document.documentElement.removeAttribute('data-theme')
})
