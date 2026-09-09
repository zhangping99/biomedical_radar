import { describe, expect, it } from 'vitest'
import { article } from '../test/articleFactory'
import { diverseSelection, matchesFocus } from './editorialFocus'

describe('editorial focus', () => {
  it('keeps clinical company news in the company focus', () => {
    const item = article({ region: 'CN', sourceType: 'company', category: 'drug_rd' })
    expect(matchesFocus(item, 'china-pharma')).toBe(true)
    expect(matchesFocus(item, 'global-pharma')).toBe(false)
  })
  it('includes domestic hospital research in both relevant groups', () => {
    const item = article({ region: 'CN', category: 'research_academic', entities: [{ type: 'hospital', name: '示例医院' }] })
    expect(matchesFocus(item, 'academic')).toBe(true)
    expect(matchesFocus(item, 'china-hospital')).toBe(true)
    expect(matchesFocus(item, 'china-policy')).toBe(false)
  })
  it('includes Chinese reimbursement policy and excludes overseas policy', () => {
    expect(matchesFocus(article({ region: 'CN', category: 'reimbursement_access' }), 'china-policy')).toBe(true)
    expect(matchesFocus(article({ region: 'US', category: 'policy_regulation' }), 'china-policy')).toBe(false)
  })
  it('caps a source without changing input order or inventing filler', () => {
    const items = ['a', 'a', 'a', 'b', 'c'].map((sourceId, index) => article({ id: String(index), sourceId }))
    expect(diverseSelection(items, 4).map(a => a.id)).toEqual(['0', '1', '3', '4'])
    expect(items).toHaveLength(5)
  })
})
