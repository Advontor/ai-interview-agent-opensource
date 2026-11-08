import request from '../utils/request'

// 知识块（对应后端 knowledge_chunk 表）
export interface KnowledgeChunk {
  id: number
  docTitle: string
  docSource?: string
  position: string
  chunkIndex: number
  content: string
  createdAt?: string
}

// 文档摘要（前端汇总展示用）
export interface DocInfo {
  docTitle: string
  position: string
  chunkCount: number
}

// 后端 GET /rag/doc/list 返回的是「不重复的文档标题」字符串数组
export function getDocTitles(): Promise<string[]> {
  return request.get('/rag/doc/list') as unknown as Promise<string[]>
}

// 按文档标题获取该文档的全部分块
export function getChunksByDoc(docTitle: string): Promise<KnowledgeChunk[]> {
  return request.get(`/rag/chunk/doc/${encodeURIComponent(docTitle)}`) as unknown as Promise<KnowledgeChunk[]>
}

// 文档列表：标题 -> 摘要（岗位从标题中推导，避免逐文档拉取全部分块）
export async function getDocList(): Promise<DocInfo[]> {
  const titles = await getDocTitles()
  return (titles || []).map((docTitle) => ({
    docTitle,
    position: docTitle.replace(/面试题库$/, '') || '通用面试',
    chunkCount: 0,
  }))
}

// 全文检索：后端参数名为 keyword（按 content 模糊匹配）
export function searchKnowledge(keyword: string): Promise<KnowledgeChunk[]> {
  return request.get('/rag/search', { params: { keyword } }) as unknown as Promise<KnowledgeChunk[]>
}
