import request from '../utils/request'

// 刷题评分结果
export interface PracticeGradeResult {
  /** 该题得分 0~100 */
  score: number
  /** AI 点评 */
  comment: string
  /** 参考答案（题库原文） */
  referenceAnswer: string
  /** 题目原文（回显） */
  question: string
}

/**
 * 批改单题：提交某道题库真题的作答，返回得分、点评与参考答案。
 * 该接口为轻量刷题专用——不需要简历、不创建面试会话、不生成报告。
 */
export function gradePractice(
  chunkId: number,
  answer: string
): Promise<PracticeGradeResult> {
  return request.post('/practice/grade', { chunkId, answer }, {
    timeout: 120000,
  }) as unknown as Promise<PracticeGradeResult>
}
