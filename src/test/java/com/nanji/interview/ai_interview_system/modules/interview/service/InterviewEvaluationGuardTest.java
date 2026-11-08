package com.nanji.interview.ai_interview_system.modules.interview.service;

import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterviewEvaluationGuardTest {

    @Test
    void recomputesWeightedScoreAndInitializesSuggestions() {
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setProfessionalAbility(80);
        evaluation.setLogicalThinking(70);
        evaluation.setCommunication(60);
        evaluation.setJobMatch(50);
        evaluation.setKnowledgeCoverage(40);

        InterviewEvaluationGuard.normalize(evaluation);

        assertEquals(67, evaluation.getScore());
        assertEquals(0, evaluation.getSuggestions().size());
    }

    @Test
    void rejectsOutOfRangeModelScore() {
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setProfessionalAbility(101);
        assertThrows(BusinessException.class, () -> InterviewEvaluationGuard.normalize(evaluation));
    }

    @Test
    void boundsReportTextAndSuggestions() {
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setProfessionalAbility(80);
        evaluation.setSummary("  summary  ");
        evaluation.setSuggestions(java.util.Arrays.asList("建议", "建议", "  ", null,
                "a".repeat(400), "1", "2", "3", "4", "5", "6"));

        InterviewEvaluationGuard.normalize(evaluation);

        assertEquals("summary", evaluation.getSummary());
        assertEquals(5, evaluation.getSuggestions().size());
        assertEquals(301, evaluation.getSuggestions().get(1).length());
    }
}
