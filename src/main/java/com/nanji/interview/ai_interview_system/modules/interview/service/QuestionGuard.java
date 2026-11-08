package com.nanji.interview.ai_interview_system.modules.interview.service;

import java.util.*;

public final class QuestionGuard {
    private QuestionGuard() {}
    private static final Set<String> QUESTION_STOP_WORDS = Set.of(
            "请", "请问", "能否", "能不能", "可以", "是否可以", "一下", "详细", "具体", "说明", "介绍",
            "讲讲", "谈谈", "你", "您", "的", "呢", "吗", "问题", "面试官"
    );

    /** Normalize labels, punctuation, whitespace and common question boilerplate. */
    private static String normalize(String text) {
        if (text == null) return "";
        String value = text.toLowerCase(Locale.ROOT).trim();
        value = value.replaceFirst("^(问题|question)\\s*[:：]\\s*", "");
        return value.replaceAll("[\\p{P}\\p{Z}\\s]", "");
    }

    /** Deterministic near-duplicate detection without an embedding service. */
    private static Set<String> semanticTokens(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return Set.of();
        Set<String> tokens = new LinkedHashSet<>();
        java.util.regex.Matcher english = java.util.regex.Pattern
                .compile("[a-z0-9]+(?:[.+#][a-z0-9]+)*")
                .matcher(normalized);
        while (english.find()) {
            String token = english.group();
            if (!QUESTION_STOP_WORDS.contains(token)) tokens.add(token);
        }
        String han = normalized.replaceAll("请问|能否|能不能|是否可以|可以|一下|详细|具体|说明|介绍|讲讲|谈谈|有哪些|哪些|如何|怎么|怎样|什么是|针对|岗位|假设|接到|一个|新任务|开展|举例|你|您|的|呢|吗|问题|面试官", "")
                .replaceAll("[a-z0-9.+#]", "");
        for (int i = 0; i + 1 < han.length(); i++) {
            String token = han.substring(i, i + 2);
            if (!QUESTION_STOP_WORDS.contains(token)) tokens.add(token);
        }
        return tokens;
    }

    private static boolean nearDuplicate(String candidate, String old) {
        Set<String> left = semanticTokens(candidate);
        Set<String> right = semanticTokens(old);
        if (left.size() < 2 || right.size() < 2) return false;
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        if (intersection.size() < 4) return false;
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        double jaccard = (double) intersection.size() / union.size();
        double containment = (double) intersection.size() / Math.min(left.size(), right.size());
        return jaccard >= 0.72 || containment >= 0.88;
    }
    public static boolean repeated(String candidate, List<String> history) {
        String normalized = normalize(candidate);
        if (normalized.isEmpty()) return true;
        if (history == null || history.isEmpty()) return false;
        return history.stream().filter(Objects::nonNull).anyMatch(old ->
                normalize(old).equals(normalized) || nearDuplicate(candidate, old));
    }
    public static String fallback(String role, List<String> history) {
        // 场景式兜底不假设用户有某种经历，岗位来自当前会话。
        for (String angle : List.of("需求澄清", "方案选择", "错误排查", "测试验证", "性能瓶颈", "数据安全",
                "边界条件", "兼容性", "上线验证", "故障恢复", "团队协作", "可维护性", "监控指标", "成本取舍", "复盘改进", "学习方法")) {
            String question = "针对" + role + "岗位，假设你接到一个新任务，你会如何开展" + angle + "？请举例说明。";
            if (!repeated(question, history)) return question;
        }
        throw new IllegalStateException("暂无未使用的兜底题，请结束当前面试");
    }
}
