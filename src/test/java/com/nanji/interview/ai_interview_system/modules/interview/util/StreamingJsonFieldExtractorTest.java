package com.nanji.interview.ai_interview_system.modules.interview.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流式 JSON 字段提取器测试：
 * 模拟模型把 JSON 切成碎片（跨 chunk 切断 key / 转义 / 中文）逐帧喂入，
 * 验证能在流未结束时就把目标字段的值逐字吐出，且最终原文完整。
 */
class StreamingJsonFieldExtractorTest {

    private static final String JSON = "{\"nextQuestion\":\"讲讲 Redis 缓存穿透的三种解法？\","
            + "\"verdict\":\"VALID\",\"action\":\"FOLLOW_UP\","
            + "\"memoryUpdate\":{\"recentSummary\":\"候选人了解基础\"}}";

    private record Result(StringBuilder acc, List<String> tokens, StreamingJsonFieldExtractor ex) {
    }

    private Result feed(String json, int chunkSize) {
        StringBuilder acc = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        StreamingJsonFieldExtractor ex = new StreamingJsonFieldExtractor("nextQuestion", t -> {
            tokens.add(t);
            acc.append(t);
        });
        for (int i = 0; i < json.length(); i += chunkSize) {
            ex.push(json.substring(i, Math.min(i + chunkSize, json.length())));
        }
        return new Result(acc, tokens, ex);
    }

    @Test
    void 流式逐字提取且增量多于一帧() {
        Result r = feed(JSON, 1);
        assertEquals("讲讲 Redis 缓存穿透的三种解法？", r.acc().toString());
        assertTrue(r.tokens().size() > 1, "应边收边吐而非等全文，实际帧数 " + r.tokens().size());
        assertTrue(r.ex().isClosed());
        assertEquals(JSON, r.ex().getBufferText());
    }

    @Test
    void 跨帧切断也能正确拼接() {
        for (int size = 1; size <= 7; size++) {
            Result r = feed(JSON, size);
            assertEquals("讲讲 Redis 缓存穿透的三种解法？", r.acc().toString(), "chunkSize=" + size);
        }
    }

    @Test
    void 值内转义被正确解码() {
        String json = "{\"nextQuestion\":\"第一行\\n带\\\"引号\\\"的问题？\",\"verdict\":\"VALID\"}";
        Result r = feed(json, 2);
        assertEquals("第一行\n带\"引号\"的问题？", r.acc().toString());
    }

    @Test
    void 代码围栏不影响提取() {
        Result r = feed("```json\n" + JSON + "\n```", 4);
        assertEquals("讲讲 Redis 缓存穿透的三种解法？", r.acc().toString());
    }

    @Test
    void 空题目时字段存在但无内容() {
        String json = "{\"nextQuestion\":\"\",\"verdict\":\"INVALID\",\"action\":\"RETRY\"}";
        Result r = feed(json, 2);
        assertEquals("", r.acc().toString());
        assertTrue(r.ex().isFound());
        assertTrue(r.ex().isClosed());
    }

    @Test
    void 字段乱序时仍能提取() {
        String json = "{\"verdict\":\"VALID\",\"action\":\"NEXT_TOPIC\",\"nextQuestion\":\"说说线程池参数？\"}";
        Result r = feed(json, 2);
        assertEquals("说说线程池参数？", r.acc().toString());
    }

    @Test
    void 非目标字段不会误触发() {
        StringBuilder acc = new StringBuilder();
        StreamingJsonFieldExtractor ex = new StreamingJsonFieldExtractor("nextQuestion", acc::append);
        ex.push("{\"verdict\":\"VALID\",\"action\":\"RETRY\",\"reason\":\"答非所问\"}");
        assertFalse(ex.isFound());
        assertEquals("", acc.toString());
    }
}
