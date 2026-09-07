import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import ArticleCard from './ArticleCard.vue'
import { article } from '../test/articleFactory'

describe('ArticleCard', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('exposes source context, title and favorite action', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: article() },
      global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
    })
    expect(wrapper.text()).toContain('示例药物获批')
    expect(wrapper.text()).toContain('A级')
    expect(wrapper.get('button').attributes('aria-label')).toBe('收藏文章')
  })
})
