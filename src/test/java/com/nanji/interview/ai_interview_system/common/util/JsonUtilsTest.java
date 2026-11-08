package com.nanji.interview.ai_interview_system.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    @Test
    void extractsBalancedObjectWhenDescriptionContainsBraces() {
        String response = "模型说明 {这不是 JSON}：{\"description\":\"使用 Map<String, Object>\",\"ok\":true} 结束";

        // The first brace is prose.  It is skipped instead of being joined to
        // the actual JSON object that follows it.
        assertEquals("{\"description\":\"使用 Map<String, Object>\",\"ok\":true}",
                JsonUtils.extractJsonObject(response));
    }

    @Test
    void keepsBracesInsideJsonStringsOutOfNestingDepth() {
        String response = "前缀\n```json\n{\"description\":\"map { key }\",\"skills\":[\"Java\"]}\n```\n后缀";

        assertEquals("{\"description\":\"map { key }\",\"skills\":[\"Java\"]}",
                JsonUtils.extractJsonObject(response));
    }

    @Test
    void handlesBomAndFenceWithoutLanguageTag() {
        assertEquals("{\"ok\":true}", JsonUtils.extractJsonObject("\uFEFF```\n{\"ok\":true}\n```"));
    }

    @Test
    void rejectsMissingOrUnclosedObjectByReturningEmptyString() {
        assertTrue(JsonUtils.extractJsonObject("no JSON here").isEmpty());
        assertTrue(JsonUtils.extractJsonObject("prefix {\"name\":\"broken}").isEmpty());
    }
}
