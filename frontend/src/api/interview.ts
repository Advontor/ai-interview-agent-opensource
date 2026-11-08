import request from '../utils/request'
import type { ResumeDTO } from './resume'
import type { QuestionReview } from './report'

// 与后端 InterviewEvaluation DTO 字段保持一致
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
  /** 逐题评分与点评 */
  questionReviews?: QuestionReview[] | null
}

// 与后端 InterviewSessionResponse DTO 字段保持一致
export interface InterviewSessionResponse {
  sessionId: number
  jobTitle: string
  difficulty: string
  currentQuestionIndex: number
  currentQuestion: string | null
  finished: boolean
  evaluation: InterviewEvaluation | null
  verdict: 'VALID' | 'PARTIAL' | 'INVALID' | 'SKIP' | null
  action: 'RETRY' | 'FOLLOW_UP' | 'NEXT_TOPIC' | 'FINISH' | null
  hint: string | null
  /** 用户配置的题目总数（来自 InterviewInitPayload.questionCount）；前端用它显示「n / total」进度条 */
  totalQuestions: number | null
}

export interface InterviewInitPayload {
  jobTitle: string
  jobDescription: string
  difficulty: string
  resume: ResumeDTO
  /** 绑定的岗位题库标题（可选）；传入即「基于知识库面试」，后端会检索该题库辅助出题 */
  knowledgeBase?: string
  /** 期望面试题数（5~20，默认 10）。后端会把它写到 AgentState 并在答满时强制结束 */
  questionCount?: number
}

// 创建面试：后端入库会话并生成第一题
export const createSession = (
  payload: InterviewInitPayload
): Promise<InterviewSessionResponse> =>
  request.post('/interview/session', payload) as unknown as Promise<InterviewSessionResponse>

// 提交回答并获得下一题（答满 5 题自动结束并带评价返回）
export const submitAnswer = (
  sessionId: number,
  answer: string
): Promise<InterviewSessionResponse> =>
  request.post('/interview/answer', { sessionId, answer }) as unknown as Promise<InterviewSessionResponse>

// ---------- 流式提交回答（SSE） ----------
// 后端逐字推送 AI 生成的下一道题，前端边收边渲染。
// 注意：原生 EventSource 不支持 POST，所以这里用 fetch + ReadableStream 读取 SSE 流。
export interface AnswerStreamCallbacks {
  /** 收到下一题的增量文本（可能回调很多次，每次一两个字） */
  onToken: (text: string) => void
  /** 本轮处理完成，返回与同步接口一致的最终结果 */
  onDone: (resp: InterviewSessionResponse) => void
  /** 流式链路彻底失败（后端已降级也失败） */
  onError?: (message: string) => void
}

interface StreamEvent {
  type: 'token' | 'done' | 'error'
  text?: string
  message?: string
  data?: InterviewSessionResponse
}

export const submitAnswerStream = async (
  sessionId: number,
  answer: string,
  cb: AnswerStreamCallbacks
): Promise<void> => {
  const res = await fetch(`/api/interview/stream/answer/${sessionId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId, answer }),
  })
  if (!res.ok || !res.body) {
    throw new Error(`流式请求失败（HTTP ${res.status}）`)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let done = false

  const handleEvent = (raw: string) => {
    // SSE 一个事件可能有多行，只取 data: 开头的行
    for (const line of raw.split('\n')) {
      const t = line.trim()
      if (!t.startsWith('data:')) continue
      const payload = t.slice(5).trim()
      if (!payload) continue
      try {
        const evt = JSON.parse(payload) as StreamEvent
        if (evt.type === 'token' && evt.text) {
          cb.onToken(evt.text)
        } else if (evt.type === 'done' && evt.data) {
          cb.onDone(evt.data)
        } else if (evt.type === 'error') {
          cb.onError?.(evt.message || '流式输出失败')
        }
      } catch (e) {
        // 单帧解析失败忽略，不中断整个流
      }
    }
  }

  while (!done) {
    const { done: readDone, value } = await reader.read()
    if (readDone) break
    // stream: true 保证中文等多字节字符被 chunk 切断时能正确拼接
    buffer += decoder.decode(value, { stream: true })
    let idx: number
    while ((idx = buffer.indexOf('\n\n')) >= 0) {
      handleEvent(buffer.slice(0, idx))
      buffer = buffer.slice(idx + 2)
    }
  }
  // 收尾：剩余不足一个完整事件的部分
  if (buffer.trim()) {
    handleEvent(buffer)
  }
  done = true
}

// 从后端恢复会话最新状态（刷新/重新进入会话时调用）
export const getSession = (sessionId: number): Promise<InterviewSessionResponse> =>
  request.get(`/interview/session/${sessionId}`) as unknown as Promise<InterviewSessionResponse>

// 结束面试：后端生成评价并保存报告
export const finishInterview = (sessionId: number): Promise<InterviewEvaluation> =>
  request.post('/interview/finish', null, {
    params: { sessionId },
  }) as unknown as Promise<InterviewEvaluation>

// 面试会话列表项（后端 InterviewSessionListItem：已从简历中解析出候选人基础信息）
export interface InterviewSessionItem {
  id: number
  jobTitle: string
  difficulty: string
  status: string
  questionCount: number
  createTime: string
  endTime: string | null
  /** 候选人姓名（简历未填写为 null） */
  candidateName: string | null
  /** 候选人年龄（简历未填写为 null） */
  candidateAge: string | null
  /** 候选人城市（简历未填写为 null） */
  candidateCity: string | null
  /** 最高学历（简历未填写为 null） */
  candidateEducation: string | null
  /** 绑定的知识库题库；为空表示标准面试 */
  knowledgeBase: string | null
  /** 报告得分；未生成报告为 null */
  score: number | null
}

// 获取用户的面试会话列表
export const getSessionList = (userId: number): Promise<InterviewSessionItem[]> =>
  request.get(`/interview/session/list/${userId}`) as unknown as Promise<InterviewSessionItem[]>

// 删除一条面试记录（连同该会话的问答消息与评估报告）
export const deleteSession = (sessionId: number): Promise<boolean> =>
  request.delete(`/interview/session/${sessionId}`) as unknown as Promise<boolean>
