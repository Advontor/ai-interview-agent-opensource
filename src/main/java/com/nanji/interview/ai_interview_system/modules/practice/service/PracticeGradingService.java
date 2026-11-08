package com.nanji.interview.ai_interview_system.modules.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.common.util.JsonUtils;
import com.nanji.interview.ai_interview_system.infrastructure.llm.LlmClient;
import com.nanji.interview.ai_interview_system.modules.practice.dto.PracticeGradeResult;
import com.nanji.interview.ai_interview_system.modules.practice.prompt.PracticeGradingPrompt;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;
import com.nanji.interview.ai_interview_system.modules.rag.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 刷题评分：单题、单轮，不涉及简历、不建面试会话、不写报告。
 * <p>
 * 流程：按 chunkId 取题库原文 → 拆出「问题 / 参考回答」→ 让模型只给「分数 + 点评」。
 */
@Service
public class PracticeGradingService {

    /** 未作答时的固定点评 */
    private static final String EMPTY_ANSWER_COMMENT = "本题未作答。建议先按自己的理解组织答案，再对照参考答案查漏补缺。";

    private final KnowledgeChunkService knowledgeChunkService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public PracticeGradingService(KnowledgeChunkService knowledgeChunkService,
                                  LlmClient llmClient,
                                  ObjectMapper objectMapper) {
        this.knowledgeChunkService = knowledgeChunkService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 批改一道题。
     *
     * @param chunkId 题目对应的 knowledge_chunk 主键
     * @param answer  求职者作答（可为空，空则直接 0 分）
     */
    public PracticeGradeResult grade(Long chunkId, String answer) {
        if (chunkId == null) {
            throw new BusinessException("缺少题目 id");
        }
        KnowledgeChunk chunk = knowledgeChunkService.getById(chunkId);
        if (chunk == null) {
            throw new BusinessException("题目不存在，请刷新后重试");
        }

        Map<String, String> fields = parseChunkContent(chunk.getContent());
        String question = firstNonBlank(fields.get("问题"), chunk.getContent());
        String reference = fields.get("参考回答");

        PracticeGradeResult result = new PracticeGradeResult();
        result.setQuestion(question);
        result.setReferenceAnswer(reference);

        String trimmed = answer == null ? "" : answer.trim();
        if (trimmed.isEmpty()) {
            result.setScore(0);
            result.setComment(EMPTY_ANSWER_COMMENT);
            return result;
        }

        GradeJson graded = gradeWithLlm(fields, trimmed);
        result.setScore(clamp(graded.score));
        result.setComment(hasText(graded.comment)
                ? graded.comment.trim()
                : "已完成批改，但模型未给出点评内容，请参考参考答案自查。");
        return result;
    }

    private GradeJson gradeWithLlm(Map<String, String> fields, String answer) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("面试题目：\n").append(nullToEmpty(fields.get("问题"))).append("\n\n");
        if (hasText(fields.get("难度"))) {
            userPrompt.append("题目难度：").append(fields.get("难度")).append("\n\n");
        }
        userPrompt.append("参考答案要点：\n")
                .append(hasText(fields.get("参考回答")) ? fields.get("参考回答") : "（题库未提供参考答案，请按该岗位的专业标准评判）")
                .append("\n\n");
        userPrompt.append("求职者作答：\n").append(answer).append("\n\n");
        userPrompt.append("请按要求的 JSON 格式给出分数与点评。");

        String raw = llmClient.chat(PracticeGradingPrompt.buildSystemPrompt(), userPrompt.toString());
        try {
            GradeJson parsed = objectMapper.readValue(JsonUtils.cleanCodeFence(raw), GradeJson.class);
            if (parsed == null) {
                throw new BusinessException("评分结果为空");
            }
            return parsed;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("评分结果解析失败，请重试", e);
        }
    }

    /**
     * 把 knowledge_chunk.content 拆成字段表。
     * 内容形如：
     * <pre>
     * 岗位标签: 产品经理, 设计/UIUX
     * 技能标签: 业务理解, 数据能力
     * 题型: how
     * 难度: advanced
     * 问题: xxxx
     * 参考回答: yyyy（可能多行）
     * </pre>
     * 首个 `键: ` 之后的、不属于任何已知键的行，视为上一个字段的续行。
     */
    private Map<String, String> parseChunkContent(String content) {
        Map<String, String> map = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return map;
        }
        String currentKey = null;
        for (String line : content.split("\n")) {
            int sep = line.indexOf(": ");
            String label = sep > 0 ? line.substring(0, sep).trim() : "";
            if (!label.isEmpty() && KNOWN_LABELS.contains(label)) {
                currentKey = label;
                map.put(label, line.substring(sep + 2));
            } else if (currentKey != null) {
                map.merge(currentKey, "\n" + line, (a, b) -> a + b);
            }
        }
        return map;
    }

    private static final java.util.Set<String> KNOWN_LABELS = java.util.Set.of(
            "岗位标签", "技能标签", "场景标签", "题型", "难度", "问题", "参考回答");

    private int clamp(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(score, 100));
    }

    private String firstNonBlank(String a, String b) {
        return hasText(a) ? a : nullToEmpty(b);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 模型返回的评分 JSON 结构 */
    public static class GradeJson {
        private Integer score;
        private String comment;

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
