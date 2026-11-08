package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.interview.dto.AgentState;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewSession;
import com.nanji.interview.ai_interview_system.modules.interview.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;

import java.util.*;

/** Builds a small, explainable graph from persisted resume and Agent state. */
@Service
public class InterviewKnowledgeGraphService {
    private final InterviewSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public InterviewKnowledgeGraphService(InterviewSessionMapper sessionMapper, ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    public record GraphResult(Long sessionId, List<Map<String, Object>> nodes,
                              List<Map<String, Object>> edges, Map<String, Integer> coverage) {}

    public GraphResult build(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("面试会话不存在");
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, Integer> coverage = new LinkedHashMap<>();
        Set<String> ids = new LinkedHashSet<>();
        String roleId = addNode(nodes, ids, "role", "岗位：" + safe(session.getJobTitle()), 20);
        addResumeNodes(session.getResumeJson(), roleId, nodes, edges, ids);
        try {
            AgentState state = session.getAgentStateJson() == null ? new AgentState()
                    : objectMapper.readValue(session.getAgentStateJson(), AgentState.class);
            if (state.getCoveredTopics() != null) for (String topic : state.getCoveredTopics()) {
                if (topic == null || topic.isBlank()) continue;
                String id = addNode(nodes, ids, "topic", clip(topic, 80), 14);
                edges.add(edge(roleId, id, "覆盖")); coverage.merge(clip(topic, 80), 1, Integer::sum);
            }
            if (state.getWeakTopics() != null) for (String topic : state.getWeakTopics()) {
                if (topic == null || topic.isBlank()) continue;
                String id = addNode(nodes, ids, "weak", clip(topic, 80), 12);
                edges.add(edge(roleId, id, "待加强")); coverage.putIfAbsent(clip(topic, 80), 0);
            }
            if (state.getAskedQuestions() != null) {
                int count = 0;
                for (String question : state.getAskedQuestions()) {
                    if (question == null || question.isBlank() || count++ >= 30) break;
                    String id = addNode(nodes, ids, "question", clip(question, 90), 10);
                    edges.add(edge(roleId, id, "已提问"));
                }
            }
        } catch (Exception ignored) { /* old sessions may have no Agent state */ }
        return new GraphResult(sessionId, nodes, edges, coverage);
    }

    private void addResumeNodes(String json, String roleId, List<Map<String, Object>> nodes,
                                List<Map<String, Object>> edges, Set<String> ids) {
        if (json == null || json.isBlank()) return;
        try {
            JsonNode root = objectMapper.readTree(json);
            for (String field : List.of("skills", "skillList", "projectList", "projects", "internshipList")) {
                JsonNode value = root.get(field); if (value == null) continue;
                if (value.isArray()) for (JsonNode item : value) addResumeItem(item, field, roleId, nodes, edges, ids);
                else if (value.isTextual()) addResumeItem(value, field, roleId, nodes, edges, ids);
            }
        } catch (Exception ignored) { }
    }
    private void addResumeItem(JsonNode item, String field, String roleId, List<Map<String, Object>> nodes,
                               List<Map<String, Object>> edges, Set<String> ids) {
        String label = item.isTextual() ? item.asText() : item.path("name").asText(item.path("title").asText(item.toString()));
        if (label.isBlank()) return;
        String id = addNode(nodes, ids, field.toLowerCase(Locale.ROOT).contains("project") ? "project" : "skill", clip(label, 90), 16);
        edges.add(edge(roleId, id, field));
    }
    private String addNode(List<Map<String, Object>> nodes, Set<String> ids, String type, String label, int size) {
        String id = type + ":" + label;
        if (ids.add(id)) nodes.add(new LinkedHashMap<>(Map.of("id", id, "name", label, "category", type, "symbolSize", size)));
        return id;
    }
    private Map<String, Object> edge(String source, String target, String label) {
        return new LinkedHashMap<>(Map.of("source", source, "target", target, "label", Map.of("show", false, "formatter", label)));
    }
    private String safe(String value) { return value == null || value.isBlank() ? "未指定" : clip(value, 80); }
    private String clip(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max) + "…"; }
}
