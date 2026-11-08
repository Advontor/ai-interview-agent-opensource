import request from '../utils/request'

// 单道题的评分与点评
export interface QuestionReview {
  /** 第几题（从 1 开始，不含开场自我介绍） */
  index: number
  question: string
  answer: string
  referenceAnswer?: string | null
  /** 该题得分 0~100；后端未提供时为 null（区别于真实 0 分） */
  score: number | null
  /** 该题的 AI 点评 */
  comment: string
}

// 面试评价（报告数据）
export interface InterviewEvaluation {
  score: number
  summary: string
  strengths: string
  weaknesses: string
  professionalAbility: number
  knowledgeCoverage: number
  jobMatch: number
  communication: number
  logicalThinking: number
  suggestions: string[]
  /** 本次面试的应聘岗位 */
  jobTitle?: string | null
  /** 本次面试的难度：EASY / MEDIUM / HARD */
  difficulty?: string | null
  /** 逐题评分与点评（旧报告可能为空数组） */
  questionReviews?: QuestionReview[] | null
}

// 面试消息（后端 sender 实际值：ai / user；前端用 interviewer / candidate 与之等价）
export interface InterviewMessage {
  id: number
  sessionId: number
  sender: 'interviewer' | 'candidate' | 'ai' | 'user'
  content: string
  /** 参考答案（仅 AI 消息在「基于知识库面试」模式下从知识库命中后填入，普通面试为空） */
  referenceAnswer?: string | null
  createTime: string
}

// 根据sessionId获取面试报告（拦截器已解包，直接返回评价对象）
export function getReportBySessionId(sessionId: number): Promise<InterviewEvaluation> {
  return request.get(`/report/session/${sessionId}`) as unknown as Promise<InterviewEvaluation>
}

// 获取面试消息列表（每道题的问答记录）
export function getMessageList(sessionId: number): Promise<InterviewMessage[]> {
  return request.get(`/interview/message/list/${sessionId}`) as unknown as Promise<InterviewMessage[]>
}

// 导出PDF报告（二进制流，保留原始响应）
export function exportReportPdf(sessionId: number) {
  return request.get(`/report/pdf/${sessionId}`, { responseType: 'blob' })
}
