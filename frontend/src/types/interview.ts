// Agent 状态枚举（严格遵守文档 5.1）
export type AgentState =
  | 'CREATED'
  | 'PREPARING'
  | 'ASKING'
  | 'WAITING_ANSWER'
  | 'ANALYZING'
  | 'EVALUATING'
  | 'FINISHED'
  | 'USER_ENDED'
  | 'FAILED';

// 第二周正式启用的 SSE 事件（文档 5.2）
export type SSEEventName =
  | 'QUESTION_STARTED'
  | 'QUESTION_CHUNK'
  | 'QUESTION_COMPLETED'
  | 'ANSWER_ANALYZED'
  | 'FOLLOW_UP'
  | 'SWITCH_TOPIC'
  | 'INTERVIEW_EVALUATING'
  | 'INTERVIEW_FINISHED'
  | 'ERROR';

// 统一 SSE 事件结构
export interface SSEMessage {
  event: SSEEventName;
  sequence: number;
  data: any; // 后端返回的具体数据，比如 { content: "..." }
}
