package com.nanji.interview.ai_interview_system.modules.knowledge;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic Agent tool facade for the bundled interview knowledge base.
 * <p>
 * It deliberately keeps retrieval outside the LLM client: callers can invoke,
 * inspect and test the tool result without relying on a provider-specific
 * function-calling protocol.  Only question directions are exposed to the
 * interviewer context; reference answers remain on the learning side.
 */
@Service
public class InterviewKnowledgeTool {
    public static final String NAME = "search_interview_knowledge";
    private final CsvInterviewKnowledgeService knowledgeService;

    public InterviewKnowledgeTool(CsvInterviewKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    public record ToolResult(String toolName, String role, String difficulty, String query,
                             List<CsvInterviewKnowledgeService.Hit> hits,
                             String interviewerContext) {}

    public ToolResult invoke(String targetRole, String difficulty, String query, int topK) {
        // Use the public search operation here so older callers and test doubles
        // can observe a single, stable tool invocation.
        List<CsvInterviewKnowledgeService.Hit> hits = knowledgeService.search(targetRole, difficulty, query, topK).hits();
        StringBuilder context = new StringBuilder();
        for (CsvInterviewKnowledgeService.Hit hit : hits) {
            context.append("[row_id=").append(hit.rowId()).append("] ")
                    .append(CsvInterviewKnowledgeService.clip(hit.question(), 240))
                    .append("；岗位：").append(hit.role())
                    .append("；难度：").append(hit.difficulty())
                    .append("；标签：").append(String.join("、", hit.tags() == null ? List.of() : hit.tags()))
                    .append('\n');
        }
        return new ToolResult(NAME, CsvInterviewKnowledgeService.normalizeRole(targetRole),
                CsvInterviewKnowledgeService.normalizeDifficulty(difficulty), query, hits,
                CsvInterviewKnowledgeService.clip(context.toString(), 1800));
    }

    /** A small OpenAI-style schema for UI/demo inspection and future native tool integration. */
    public Map<String, Object> definition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("targetRole", Map.of("type", "string", "description", "应聘岗位"));
        properties.put("difficulty", Map.of("type", "string", "enum", List.of("EASY", "MEDIUM", "HARD")));
        properties.put("query", Map.of("type", "string", "description", "当前问题或知识点"));
        properties.put("topK", Map.of("type", "integer", "minimum", 1, "maximum", CsvInterviewKnowledgeService.MAX_TOP_K));
        return Map.of(
                "name", NAME,
                "description", "从本地面试题库检索与岗位和难度匹配的题目方向。结果不是候选人事实或标准答案。",
                "parameters", Map.of("type", "object", "properties", properties,
                        "required", List.of("query")));
    }
}
