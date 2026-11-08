package com.nanji.interview.ai_interview_system.common.util;

/**
 * JSON 相关小工具。
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 去掉 LLM 返回内容中可能包裹的 Markdown 代码块标记（```json / ```）。
     */
    public static String cleanCodeFence(String json) {
        if (json == null) {
            return "";
        }
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
