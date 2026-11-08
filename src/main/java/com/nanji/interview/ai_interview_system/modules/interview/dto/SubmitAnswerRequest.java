package com.nanji.interview.ai_interview_system.modules.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 提交回答请求。
 */
public class SubmitAnswerRequest {

    /** 面试会话 id（interview_session 主键） */
    @NotNull(message = "面试会话 id 不能为空")
    private Long sessionId;

    /** 候选人的回答内容 */
    @NotBlank(message = "回答内容不能为空")
    private String answer;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
