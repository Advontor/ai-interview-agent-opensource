package com.nanji.interview.ai_interview_system.modules.practice.dto;

/**
 * 刷题评分请求：对某道题库真题提交一次作答。
 */
public class PracticeGradeRequest {

    /** 题目对应的 knowledge_chunk 主键（保证题目与参考答案来自题库原文） */
    private Long chunkId;

    /** 候选人的作答内容 */
    private String answer;

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
