import type { Article } from '../domain/article'

export const focusTopics = [
  { id: 'academic', label: '学术研究' },
  { id: 'china-policy', label: '中国政策' },
  { id: 'china-pharma', label: '国内药企' },
  { id: 'global-pharma', label: '国外药企' },
  { id: 'china-hospital', label: '国内医院' },
] as const
export type FocusTopic = (typeof focusTopics)[number]['id']

export function matchesFocus(article: Article, topic: FocusTopic): boolean {
  switch (topic) {
    case 'academic': return article.sourceType === 'journal' || article.category === 'research_academic'
      || article.eventTypes.includes('research_publication')
    case 'china-policy': return article.region === 'CN' && ['policy_regulation', 'reimbursement_access'].includes(article.category)
    case 'china-pharma': return article.region === 'CN' && article.sourceType === 'company'
    case 'global-pharma': return article.region !== 'CN' && article.sourceType === 'company'
    case 'china-hospital': return article.region === 'CN' && article.entities.some(entity => entity.type === 'hospital')
  }
}

// Input is already ranked. Cap each source without inventing scores or changing the latest feed.
export function diverseSelection(articles: Article[], limit: number, perSource = 2): Article[] {
  const counts = new Map<string, number>()
  return articles.filter(article => {
    const count = counts.get(article.sourceId) ?? 0
    if (count >= perSource) return false
    counts.set(article.sourceId, count + 1)
    return true
  }).slice(0, limit)
}
