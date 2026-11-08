package com.nanji.interview.ai_interview_system.modules.interview.service;

import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 服务端报告边界校验。模型只负责给出五个维度的证据型评分，综合分按固定权重计算，
 * 避免把模型自由输出直接当作最终分数。
 */
public final class InterviewEvaluationGuard {

    private InterviewEvaluationGuard() {
    }

    public static InterviewEvaluation normalize(InterviewEvaluation evaluation) {
        if (evaluation == null) throw new BusinessException("评价结果为空，面试未结束，请稍后重试");
        checkRange("professionalAbility", evaluation.getProfessionalAbility());
        checkRange("knowledgeCoverage", evaluation.getKnowledgeCoverage());
        checkRange("jobMatch", evaluation.getJobMatch());
        checkRange("communication", evaluation.getCommunication());
        checkRange("logicalThinking", evaluation.getLogicalThinking());
        int score = Math.round(
                evaluation.getProfessionalAbility() * 0.40f
                        + evaluation.getLogicalThinking() * 0.20f
                        + evaluation.getCommunication() * 0.15f
                        + evaluation.getJobMatch() * 0.15f
                        + evaluation.getKnowledgeCoverage() * 0.10f);
        evaluation.setScore(score);
        evaluation.setSummary(clip(evaluation.getSummary(), 1200));
        evaluation.setStrengths(clip(evaluation.getStrengths(), 1600));
        evaluation.setWeaknesses(clip(evaluation.getWeaknesses(), 1600));
        List<String> suggestions = evaluation.getSuggestions() == null
                ? List.of()
                : evaluation.getSuggestions().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> clip(value, 300))
                .distinct()
                .limit(5)
                .toList();
        evaluation.setSuggestions(new ArrayList<>(suggestions));
        return evaluation;
    }

    private static String clip(String value, int maxLength) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
    }

    private static void checkRange(String dimension, int value) {
        if (value < 0 || value > 100)
            throw new BusinessException("评价维度 " + dimension + " 必须为0至100分，面试未结束，请重试");
    }
}
