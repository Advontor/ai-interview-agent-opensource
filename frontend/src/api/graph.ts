import request from '../utils/request'

// 图谱节点
export interface GraphNode {
  id: number
  nodeType: string // JOB/SKILL/KNOWLEDGE/QUESTION
  name: string
  description?: string
}

// 图谱边
export interface GraphEdge {
  id: number
  sourceNodeId: number
  targetNodeId: number
  relationType: string
  weight?: number
}

/**
 * 统一的列表请求：request 响应拦截器已经把 {code,data} 解包成 data，
 * 这里把类型断言成数组；接口失败（如图谱表还没建）时降级为空数组。
 */
async function fetchList<T>(url: string): Promise<T[]> {
  try {
    return (await request.get(url)) as unknown as T[]
  } catch {
    return []
  }
}

// 按类型获取节点
export function getNodesByType(nodeType: string): Promise<GraphNode[]> {
  return fetchList<GraphNode>(`/graph/node/type/${nodeType}`)
}

// 获取所有节点（并行获取四种类型）
export async function getAllNodes() {
  const [jobs, skills, knowledges, questions] = await Promise.all([
    getNodesByType('JOB'),
    getNodesByType('SKILL'),
    getNodesByType('KNOWLEDGE'),
    getNodesByType('QUESTION')
  ])
  return { jobs, skills, knowledges, questions }
}

// 获取节点的所有边（作为源和目标）
export async function getEdgesForNode(nodeId: number): Promise<GraphEdge[]> {
  const [sourceEdges, targetEdges] = await Promise.all([
    fetchList<GraphEdge>(`/graph/edge/source/${nodeId}`),
    fetchList<GraphEdge>(`/graph/edge/target/${nodeId}`)
  ])
  return [...sourceEdges, ...targetEdges]
}
