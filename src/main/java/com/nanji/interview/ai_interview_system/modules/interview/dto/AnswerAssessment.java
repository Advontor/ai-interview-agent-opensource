package com.nanji.interview.ai_interview_system.modules.interview.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一轮回答的判定 + 下一步决策 + 下一题（一次 DeepSeek 调用同时返回）。
 */
@Data
public class AnswerAssessment {

    /** 回答判定：VALID / PARTIAL / INVALID / SKIP */
    private String verdict;

    /** 下一步动作：RETRY / FOLLOW_UP / NEXT_TOPIC / FINISH */
    private String action;

    /** 当前讨论的知识点（简短名词短语） */
    private String topic;

    /** 判定与决策理由（一句话） */
    private String reason;

    /** 下一道面试题（RETRY / FINISH 时为空字符串） */
    private String nextQuestion;

    /** 给候选人的提示（仅 RETRY 时返回） */
    private String hint;

    /** 模型建议的记忆更新 */
    private MemoryUpdate memoryUpdate = new MemoryUpdate();

    @Data
    public static class MemoryUpdate {
        private List<String> coveredTopics = new ArrayList<>();
        private List<String> weakTopics = new ArrayList<>();
        private String recentSummary;
    }
}
