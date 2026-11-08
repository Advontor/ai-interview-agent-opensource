import request from '../utils/request'

export interface KnowledgeRecommendation {
  resourceId: string
  title: string
  summary: string
  resourceType: string
  source: string
  role: string
  difficulty: string
  tags: string[]
  score: number
  reason: string
}

export interface KnowledgeRecommendationResult {
  role: string
  difficulty: string
  query: string
  recommendations: KnowledgeRecommendation[]
  mode: string
  version: string
}

/**
 * Retrieves study/reference items from the local interview knowledge base.
 * This endpoint is deliberately independent from answer submission so a
 * transient knowledge-base failure never blocks the interview.
 */
export const recommendInterviewResources = (
  targetRole: string,
  difficulty: string,
  question: string,
  answer: string,
  weakTopics = '',
  topK = 3,
): Promise<KnowledgeRecommendationResult> => request.get('/knowledge/interview-rag/recommend', {
  params: { targetRole, difficulty, question, answer, weakTopics, topK },
}) as unknown as Promise<KnowledgeRecommendationResult>

