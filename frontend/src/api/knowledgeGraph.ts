import request from '../utils/request'

export interface KnowledgeGraphResult { sessionId: number; nodes: any[]; edges: any[]; coverage: Record<string, number> }
export const getKnowledgeGraph = (sessionId: number): Promise<KnowledgeGraphResult> =>
  request.get(`/knowledge/graphs/${sessionId}`) as unknown as Promise<KnowledgeGraphResult>
