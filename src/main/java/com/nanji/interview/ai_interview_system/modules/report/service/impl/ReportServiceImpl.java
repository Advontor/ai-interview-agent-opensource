package com.nanji.interview.ai_interview_system.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.report.entity.Report;
import com.nanji.interview.ai_interview_system.modules.report.mapper.ReportMapper;
import com.nanji.interview.ai_interview_system.modules.report.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private final ObjectMapper objectMapper;

    public ReportServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void saveEvaluation(Long sessionId, InterviewEvaluation evaluation) {
        Report report = new Report();
        report.setSessionId(sessionId);
        report.setScore(evaluation.getScore());
        report.setAdvantage(evaluation.getStrengths());
        report.setDisadvantage(evaluation.getWeaknesses());
        report.setSuggestion(suggestionsToJson(evaluation.getSuggestions()));
        report.setSummary(evaluation.getSummary());
        report.setProfessionalAbility(evaluation.getProfessionalAbility());
        report.setKnowledgeCoverage(evaluation.getKnowledgeCoverage());
        report.setJobMatch(evaluation.getJobMatch());
        report.setCommunication(evaluation.getCommunication());
        report.setLogicalThinking(evaluation.getLogicalThinking());
        report.setJobTitle(evaluation.getJobTitle());
        report.setDifficulty(evaluation.getDifficulty());
        report.setQuestionReviews(questionReviewsToJson(evaluation.getQuestionReviews()));
        report.setCreateTime(LocalDateTime.now());
        save(report);
    }

    @Override
    public InterviewEvaluation getEvaluationBySessionId(Long sessionId) {
        Report report = lambdaQuery()
                .eq(Report::getSessionId, sessionId)
                .one();
        if (report == null) {
            throw new BusinessException("该面试暂无报告");
        }

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setScore(report.getScore() == null ? 0 : report.getScore());
        evaluation.setSummary(report.getSummary());
        evaluation.setStrengths(report.getAdvantage());
        evaluation.setWeaknesses(report.getDisadvantage());
        evaluation.setSuggestions(parseSuggestions(report.getSuggestion()));
        evaluation.setProfessionalAbility(nullToZero(report.getProfessionalAbility()));
        evaluation.setKnowledgeCoverage(nullToZero(report.getKnowledgeCoverage()));
        evaluation.setJobMatch(nullToZero(report.getJobMatch()));
        evaluation.setCommunication(nullToZero(report.getCommunication()));
        evaluation.setLogicalThinking(nullToZero(report.getLogicalThinking()));
        evaluation.setJobTitle(report.getJobTitle());
        evaluation.setDifficulty(report.getDifficulty());
        evaluation.setQuestionReviews(parseQuestionReviews(report.getQuestionReviews()));
        return evaluation;
    }

    @Override
    @Transactional
    public void deleteBySessionId(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        remove(new LambdaQueryWrapper<Report>().eq(Report::getSessionId, sessionId));
    }

    @Override
    public Map<Long, Integer> findScoresBySessionIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        List<Report> reports = list(new LambdaQueryWrapper<Report>()
                .in(Report::getSessionId, sessionIds)
                .select(Report::getSessionId, Report::getScore));
        Map<Long, Integer> scores = new HashMap<>();
        for (Report report : reports) {
            if (report.getSessionId() != null && report.getScore() != null) {
                scores.put(report.getSessionId(), report.getScore());
            }
        }
        return scores;
    }

    private String suggestionsToJson(List<String> suggestions) {
        if (suggestions == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(suggestions);
        } catch (Exception e) {
            throw new BusinessException("改进建议序列化失败", e);
        }
    }

    private List<String> parseSuggestions(String suggestionJson) {
        if (suggestionJson == null || suggestionJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> list = objectMapper.readValue(
                    suggestionJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            // 旧数据可能是纯文本，按换行拆分兜底
            return new ArrayList<>(List.of(suggestionJson.split("\\R")));
        }
    }

    /** 逐题评价序列化为 JSON；为空时存 null（前端据此区分「旧报告无逐题点评」与「真实 0 分」） */
    private String questionReviewsToJson(List<InterviewEvaluation.QuestionReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(reviews);
        } catch (Exception e) {
            throw new BusinessException("逐题评价序列化失败", e);
        }
    }

    /** 反序列化逐题评价；解析失败返回空列表而非抛错，避免旧数据打不开报告 */
    private List<InterviewEvaluation.QuestionReview> parseQuestionReviews(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<InterviewEvaluation.QuestionReview> list = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, InterviewEvaluation.QuestionReview.class)
            );
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
