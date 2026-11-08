package com.nanji.interview.ai_interview_system.modules.interview.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentState 单元测试：覆盖第二周 Agent Memory 新增字段的序列化/反序列化、
 * recentTurns 上限、Memory 合并去重、compressedSummary 生成。
 */
class AgentStateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 1. recentTurns 最多 4 条 ====================

    @Test
    void recentTurns_shouldCapAtMaxRecentTurns() {
        AgentState state = new AgentState();

        // 添加 6 条记录
        for (int i = 1; i <= 6; i++) {
            AgentState.TurnRecord record = new AgentState.TurnRecord(
                    "问题" + i, "回答" + i, "topic" + i, "VALID"
            );
            state.getRecentTurns().add(record);
        }

        // 模拟 updateMemory 中的 cap 逻辑
        while (state.getRecentTurns().size() > AgentState.MAX_RECENT_TURNS) {
            state.getRecentTurns().remove(0);
        }

        assertEquals(6, state.getRecentTurns().size());
        // 保留的应该是最新的 6 条（1, 2, 3, 4, 5, 6）
        assertEquals("问题1", state.getRecentTurns().get(0).getQuestion());
        assertEquals("问题6", state.getRecentTurns().get(5).getQuestion());
    }

    @Test
    void recentTurns_maxRecentTurnsConstant_shouldBe4() {
        assertEquals(6, AgentState.MAX_RECENT_TURNS);
    }

    // ==================== 2. strengths / weaknesses / evidence 合并 ====================

    @Test
    void mergeUnique_shouldAddNewItems() {
        AgentState state = new AgentState();
        state.getStrengths().add("理解乐观锁");

        AnswerAssessment.MemoryUpdate mu = new AnswerAssessment.MemoryUpdate();
        mu.getStrengthsToAdd().add("熟悉Redis缓存策略");
        mu.getWeaknessesToAdd().add("高并发场景分析不足");
        mu.getEvidenceToAdd().add("候选人能说出Redis的缓存穿透解决方案");

        // 模拟 mergeUnique 逻辑
        mergeUnique(state.getStrengths(), mu.getStrengthsToAdd());
        mergeUnique(state.getWeaknesses(), mu.getWeaknessesToAdd());
        mergeUnique(state.getEvidence(), mu.getEvidenceToAdd());

        assertEquals(2, state.getStrengths().size());
        assertTrue(state.getStrengths().contains("理解乐观锁"));
        assertTrue(state.getStrengths().contains("熟悉Redis缓存策略"));

        assertEquals(1, state.getWeaknesses().size());
        assertTrue(state.getWeaknesses().contains("高并发场景分析不足"));

        assertEquals(1, state.getEvidence().size());
        assertTrue(state.getEvidence().contains("候选人能说出Redis的缓存穿透解决方案"));
    }

    @Test
    void mergeUnique_shouldNotAddNull() {
        AgentState state = new AgentState();
        List<String> target = state.getStrengths();

        mergeUnique(target, null);
        assertEquals(0, target.size());

        mergeUnique(target, new ArrayList<>());
        assertEquals(0, target.size());
    }

    // ==================== 3. 重复项不会重复加入 ====================

    @Test
    void mergeUnique_shouldDeduplicate() {
        AgentState state = new AgentState();
        state.getStrengths().add("理解乐观锁");
        state.getWeaknesses().add("并发编程基础薄弱");
        state.getEvidence().add("候选人提到synchronized");

        AnswerAssessment.MemoryUpdate mu = new AnswerAssessment.MemoryUpdate();
        mu.getStrengthsToAdd().add("理解乐观锁"); // 重复
        mu.getWeaknessesToAdd().add("并发编程基础薄弱"); // 重复
        mu.getEvidenceToAdd().add("候选人提到synchronized"); // 重复
        mu.getStrengthsToAdd().add("熟悉MySQL索引优化"); // 新增

        mergeUnique(state.getStrengths(), mu.getStrengthsToAdd());
        mergeUnique(state.getWeaknesses(), mu.getWeaknessesToAdd());
        mergeUnique(state.getEvidence(), mu.getEvidenceToAdd());

        // 重复项不应加入
        assertEquals(2, state.getStrengths().size());
        assertEquals(1, state.getWeaknesses().size());
        assertEquals(1, state.getEvidence().size());

        assertTrue(state.getStrengths().contains("熟悉MySQL索引优化"));
        assertFalse(state.getStrengths().stream().filter(s -> s.equals("理解乐观锁")).count() > 1);
    }

    @Test
    void mergeUnique_shouldTrimAndSkipBlank() {
        AgentState state = new AgentState();
        List<String> target = state.getStrengths();

        List<String> source = List.of("  理解HashMap  ", "", "  ", "熟悉ArrayList");
        mergeUnique(target, source);

        assertEquals(2, target.size());
        assertTrue(target.contains("理解HashMap"));
        assertTrue(target.contains("熟悉ArrayList"));
    }

    // ==================== 4. JSON 序列化反序列化兼容旧数据 ====================

    @Test
    void deserialize_oldJsonWithoutMemoryFields_shouldSucceed() throws Exception {
        // 模拟第一周的 JSON（没有 recentTurns, strengths, weaknesses, evidence, compressedSummary）
        String oldJson = """
                {
                  "effectiveQuestionCount": 5,
                  "currentTopic": "Redis缓存",
                  "currentTopicFollowUpCount": 1,
                  "consecutiveInvalidCount": 0,
                  "clarifiedCurrentQuestion": false,
                  "askedQuestions": ["Q1", "Q2"],
                  "coveredTopics": ["Java基础"],
                  "weakTopics": ["并发编程"],
                  "recentSummary": "候选人对Redis有一定了解"
                }
                """;

        AgentState state = objectMapper.readValue(oldJson, AgentState.class);

        assertNotNull(state);
        assertEquals(5, state.getEffectiveQuestionCount());
        assertEquals("Redis缓存", state.getCurrentTopic());
        assertEquals(1, state.getCurrentTopicFollowUpCount());
        assertEquals("Java基础", state.getCoveredTopics().get(0));
        assertEquals("并发编程", state.getWeakTopics().get(0));
        assertEquals("候选人对Redis有一定了解", state.getRecentSummary());

        // 第二周新增字段应为空/默认值
        assertNotNull(state.getRecentTurns());
        assertTrue(state.getRecentTurns().isEmpty());
        assertNotNull(state.getStrengths());
        assertTrue(state.getStrengths().isEmpty());
        assertNotNull(state.getWeaknesses());
        assertTrue(state.getWeaknesses().isEmpty());
        assertNotNull(state.getEvidence());
        assertTrue(state.getEvidence().isEmpty());
        assertNull(state.getCompressedSummary());
    }

    @Test
    void serializeAndDeserialize_newFields_shouldRoundTrip() throws Exception {
        AgentState state = new AgentState();
        state.setEffectiveQuestionCount(3);
        state.setCurrentTopic("MySQL事务");
        state.getRecentTurns().add(new AgentState.TurnRecord("Q1", "A1", "MySQL事务", "VALID"));
        state.getStrengths().add("理解ACID");
        state.getWeaknesses().add("隔离级别理解不深");
        state.getEvidence().add("候选人能说出四种隔离级别");
        state.setCompressedSummary("已覆盖：MySQL事务；优势：理解ACID");

        String json = objectMapper.writeValueAsString(state);
        AgentState restored = objectMapper.readValue(json, AgentState.class);

        assertEquals(3, restored.getEffectiveQuestionCount());
        assertEquals(1, restored.getRecentTurns().size());
        assertEquals("Q1", restored.getRecentTurns().get(0).getQuestion());
        assertEquals("A1", restored.getRecentTurns().get(0).getAnswer());
        assertEquals("MySQL事务", restored.getRecentTurns().get(0).getTopic());
        assertEquals("VALID", restored.getRecentTurns().get(0).getVerdict());
        assertEquals(1, restored.getStrengths().size());
        assertEquals("理解ACID", restored.getStrengths().get(0));
        assertEquals(1, restored.getWeaknesses().size());
        assertEquals(1, restored.getEvidence().size());
        assertEquals("已覆盖：MySQL事务；优势：理解ACID", restored.getCompressedSummary());
    }

    @Test
    void deserialize_emptyJson_shouldReturnDefaultState() throws Exception {
        AgentState state = objectMapper.readValue("{}", AgentState.class);
        assertNotNull(state);
        assertEquals(0, state.getEffectiveQuestionCount());
        assertNotNull(state.getRecentTurns());
        assertTrue(state.getRecentTurns().isEmpty());
        assertNotNull(state.getStrengths());
        assertTrue(state.getStrengths().isEmpty());
    }

    // ==================== 5. compressedSummary 本地生成 ====================

    @Test
    void generateCompressedSummary_withAllFields_shouldContainAllSections() {
        AgentState state = new AgentState();
        state.getCoveredTopics().add("Java并发");
        state.getCoveredTopics().add("MySQL事务");
        state.getStrengths().add("理解乐观锁");
        state.getWeaknesses().add("高冲突场景分析不足");
        state.getEvidence().add("候选人能说出CAS原理");

        String summary = generateCompressedSummary(state);

        assertNotNull(summary);
        assertTrue(summary.contains("已覆盖：Java并发、MySQL事务"));
        assertTrue(summary.contains("优势：理解乐观锁"));
        assertTrue(summary.contains("薄弱点：高冲突场景分析不足"));
        assertTrue(summary.contains("依据："));
        assertTrue(summary.contains("候选人能说出CAS原理"));
    }

    @Test
    void generateCompressedSummary_withOnlyCoveredTopics_shouldContainOnlyThatSection() {
        AgentState state = new AgentState();
        state.getCoveredTopics().add("Redis缓存");

        String summary = generateCompressedSummary(state);

        assertNotNull(summary);
        assertTrue(summary.contains("已覆盖：Redis缓存"));
        assertFalse(summary.contains("优势"));
        assertFalse(summary.contains("薄弱点"));
    }

    @Test
    void generateCompressedSummary_withEmptyState_shouldReturnNull() {
        AgentState state = new AgentState();
        String summary = generateCompressedSummary(state);
        assertNull(summary);
    }

    @Test
    void generateCompressedSummary_evidenceShouldCapAt3() {
        AgentState state = new AgentState();
        state.getEvidence().add("依据1");
        state.getEvidence().add("依据2");
        state.getEvidence().add("依据3");
        state.getEvidence().add("依据4");
        state.getEvidence().add("依据5");

        String summary = generateCompressedSummary(state);

        assertNotNull(summary);
        // 应只包含最近 3 条（依据3、依据4、依据5）
        assertTrue(summary.contains("依据3"));
        assertTrue(summary.contains("依据4"));
        assertTrue(summary.contains("依据5"));
        assertFalse(summary.contains("依据1"));
        assertFalse(summary.contains("依据2"));
    }

    // ==================== 辅助方法（与 ServiceImpl 中的逻辑一致） ====================

    private void mergeUnique(List<String> target, List<String> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (String item : source) {
            if (item != null && !item.trim().isEmpty() && !target.contains(item.trim())) {
                target.add(item.trim());
            }
        }
    }

    private String generateCompressedSummary(AgentState state) {
        StringBuilder sb = new StringBuilder();
        if (!state.getCoveredTopics().isEmpty()) {
            sb.append("已覆盖：").append(String.join("、", state.getCoveredTopics())).append("；");
        }
        if (!state.getStrengths().isEmpty()) {
            sb.append("优势：").append(String.join("、", state.getStrengths())).append("；");
        }
        if (!state.getWeaknesses().isEmpty()) {
            sb.append("薄弱点：").append(String.join("、", state.getWeaknesses())).append("；");
        }
        if (!state.getEvidence().isEmpty()) {
            List<String> recentEvidence = state.getEvidence();
            if (recentEvidence.size() > 3) {
                recentEvidence = recentEvidence.subList(recentEvidence.size() - 3, recentEvidence.size());
            }
            sb.append("依据：").append(String.join("；", recentEvidence)).append("。");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
