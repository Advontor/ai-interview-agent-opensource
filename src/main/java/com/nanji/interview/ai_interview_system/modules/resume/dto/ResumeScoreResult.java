package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历评分及面试配置建议。
 *
 * 分数由 ResumeScoringService 按固定规则计算，模型只负责上游结构化解析，
 * 因此同一份简历在相同岗位下结果稳定且可解释。
 */
@Data
public class ResumeScoreResult {

    /** 0-100 的综合分数。 */
    private int totalScore;

    /** 分项分数，key 使用稳定的英文标识供前端绘图。 */
    private Map<String, Integer> breakdown = new LinkedHashMap<>();

    /** 每个分项的计算说明。 */
    private Map<String, String> explanations = new LinkedHashMap<>();

    private List<String> strengths = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private List<String> recommendedRoles = new ArrayList<>();
    private List<String> recommendedTopics = new ArrayList<>();
    private List<String> techStack = new ArrayList<>();

    /** 可直接复制到面试配置页的默认配置。 */
    private ResumeInterviewDefaults defaults = new ResumeInterviewDefaults();

    @Data
    public static class ResumeInterviewDefaults {
        private String targetRole;
        private String targetLevel = "MID";
        private String difficulty = "MEDIUM";
        private List<String> focusTopics = new ArrayList<>();
        private List<String> interviewStyles = new ArrayList<>();
    }
}
