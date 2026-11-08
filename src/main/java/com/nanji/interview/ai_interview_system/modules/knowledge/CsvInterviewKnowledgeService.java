package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/** Local, deterministic lexical retrieval over the checked-in CSV-derived resource. */
@Service
public class CsvInterviewKnowledgeService {
    public static final int MAX_TOP_K = 5;
    // A single incidental tag match is too weak; exact phrases or two meaningful terms pass.
    private static final double MIN_SCORE = 6.0;
    private static final Pattern ENGLISH = Pattern.compile("[a-z0-9]+(?:[.+#][a-z0-9]+)*");
    private static final Pattern HAN = Pattern.compile("[\\p{IsHan}]+");
    private final List<Entry> entries;

    public CsvInterviewKnowledgeService(ObjectMapper mapper) {
        try (var input = new GZIPInputStream(new ClassPathResource("knowledge/interview-rag-v1.jsonl.gz").getInputStream());
             var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<Entry> loaded = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) loaded.add(mapper.readValue(line, Entry.class));
            }
            if (loaded.isEmpty()) throw new IllegalStateException("CSV 知识库为空");
            entries = List.copyOf(loaded);
        } catch (Exception ex) {
            throw new IllegalStateException("无法加载本地 CSV 知识库资源", ex);
        }
    }

    public record Entry(String rowId, String question, String answer, String role,
                        String difficulty, List<String> tags, String retrievalText, String questionType) {
        String searchable() {
            return (question + " " + String.join(" ", tags) + " " + retrievalText).toLowerCase(Locale.ROOT);
        }
    }

    public record Hit(String rowId, String question, String answer, String role,
                      String difficulty, List<String> tags, double score) {}

    public record SearchResult(String role, String difficulty, String query,
                               List<Hit> hits, String context) {}
    /**
     * A user-facing learning item derived from a retrieval hit.  The bundled
     * question bank is intentionally exposed as a reference item rather than
     * an authoritative answer: the UI can explain why it was recommended and
     * the interview prompt can keep it clearly separated from candidate facts.
     */
    public record Recommendation(String resourceId, String title, String summary,
                                 String resourceType, String source, String role,
                                 String difficulty, List<String> tags, double score,
                                 String reason) {}

    public record RecommendationResult(String role, String difficulty, String query,
                                       List<Recommendation> recommendations,
                                       String mode, String version) {}
    public record Stats(String mode, String version, int entryCount, List<String> roles,
                        List<String> difficulties, String sourceName) {}

    public int size() { return entries.size(); }

    public Stats stats() {
        return new Stats("LOCAL_LEXICAL_RAG", "interview-rag-v1", entries.size(),
                entries.stream().map(Entry::role).filter(Objects::nonNull).distinct().sorted().toList(),
                entries.stream().map(Entry::difficulty).filter(Objects::nonNull).distinct().sorted().toList(),
                "interview_rag_structured_v1(1)(1).csv");
    }

    public List<Hit> retrieve(String requestedRole, String difficulty, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        int limit = Math.max(1, Math.min(MAX_TOP_K, topK));
        String role = normalizeRole(requestedRole);
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty()) return List.of();
        PriorityQueue<Scored> best = new PriorityQueue<>(Comparator.comparingDouble(Scored::score)
                .thenComparing(s -> s.entry().rowId()));
        for (Entry entry : entries) {
            if (!role.isBlank() && !entry.role().equals(role)) continue;
            if (!normalizedDifficulty.isBlank() && !entry.difficulty().equals(normalizedDifficulty)) continue;
            double score = score(entry, queryTerms, query);
            if (score < MIN_SCORE) continue;
            best.offer(new Scored(entry, score));
            if (best.size() > limit) best.poll();
        }
        List<Scored> sorted = new ArrayList<>(best);
        sorted.sort(Comparator.comparingDouble(Scored::score).reversed().thenComparing(s -> s.entry().rowId()));
        return sorted.stream().map(s -> new Hit(s.entry().rowId(), s.entry().question(), s.entry().answer(),
                s.entry().role(), s.entry().difficulty(), s.entry().tags(), round(s.score()))).toList();
    }

    public SearchResult search(String requestedRole, String difficulty, String query, int topK) {
        List<Hit> hits = retrieve(requestedRole, difficulty, query, topK);
        StringBuilder context = new StringBuilder();
        for (Hit hit : hits) {
            context.append("[row_id=").append(hit.rowId()).append("] ")
                    .append(hit.question()).append("\n参考答案摘录：")
                    .append(clip(hit.answer(), 420)).append("\n岗位：")
                    .append(hit.role()).append("；难度：").append(hit.difficulty())
                    .append("；标签：").append(String.join("、", hit.tags())).append("\n\n");
        }
        return new SearchResult(normalizeRole(requestedRole), normalizeDifficulty(difficulty), query,
                hits, clip(context.toString(), 3000));
    }

    /**
     * Retrieves study/reference items using the current question together
     * with the candidate answer and any known weak topics.  Keeping this
     * operation deterministic makes it safe to call after every answer and
     * allows the frontend to retry without changing the interview state.
     */
    public RecommendationResult recommend(String requestedRole, String difficulty,
                                           String question, String answer,
                                           String weakTopics, int topK) {
        String query = buildAnswerQuery(question, answer, weakTopics);
        List<Hit> hits = retrieve(requestedRole, difficulty, query, topK);
        String normalizedRole = normalizeRole(requestedRole);
        String normalizedDifficulty = normalizeDifficulty(difficulty);
        List<Recommendation> recommendations = hits.stream().map(hit -> {
            String reason = recommendationReason(question, answer, weakTopics, hit);
            return new Recommendation(
                    "interview-rag:" + hit.rowId(),
                    clip(hit.question(), 180),
                    clip(hit.answer(), 420),
                    "面试知识参考",
                    "interview-rag-v1 / " + hit.rowId(),
                    hit.role(),
                    hit.difficulty(),
                    hit.tags() == null ? List.of() : List.copyOf(hit.tags()),
                    hit.score(),
                    reason);
        }).toList();
        return new RecommendationResult(normalizedRole, normalizedDifficulty, query,
                recommendations, "LOCAL_LEXICAL_RAG", "interview-rag-v1");
    }

    private String buildAnswerQuery(String question, String answer, String weakTopics) {
        StringBuilder query = new StringBuilder();
        appendQueryPart(query, question, 1400);
        appendQueryPart(query, answer, 2200);
        appendQueryPart(query, weakTopics, 600);
        return clip(query.toString(), 4000);
    }

    private void appendQueryPart(StringBuilder query, String value, int max) {
        if (value == null || value.isBlank()) return;
        if (!query.isEmpty()) query.append(' ');
        query.append(clip(value.trim(), max));
    }

    private String recommendationReason(String question, String answer, String weakTopics, Hit hit) {
        if (weakTopics != null && !weakTopics.isBlank())
            return "与你当前回答暴露出的薄弱知识点相关，可用于补充复习：" + clip(weakTopics, 100);
        if (answer != null && !answer.isBlank())
            return "根据当前问题和回答中的关键词匹配，建议对照这条参考资料复习。";
        return "根据当前面试问题匹配到的参考资料。";
    }

    private double score(Entry entry, Set<String> queryTerms, String query) {
        String question = entry.question().toLowerCase(Locale.ROOT);
        String searchable = entry.searchable();
        double value = 0;
        for (String term : queryTerms) {
            if (question.contains(term)) value += 5;
            else if (searchable.contains(term)) value += 2;
        }
        String phrase = query.trim().toLowerCase(Locale.ROOT);
        if (phrase.length() >= 3 && question.contains(phrase)) value += 6;
        return value;
    }

    static Set<String> terms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher english = ENGLISH.matcher(text.toLowerCase(Locale.ROOT));
        while (english.find()) if (english.group().length() >= 2) terms.add(english.group());
        Matcher han = HAN.matcher(text);
        while (han.find()) {
            String run = han.group();
            if (run.length() == 1) terms.add(run);
            for (int i = 0; i + 1 < run.length(); i++) terms.add(run.substring(i, i + 2));
        }
        return terms;
    }

    public static String normalizeRole(String role) {
        if (role == null) return "";
        String value = role.trim().toLowerCase(Locale.ROOT);
        if (value.contains("后端") || value.contains("backend") || value.contains("java")) return "后端开发";
        if (value.contains("前端") || value.contains("frontend")) return "前端开发";
        if (value.contains("产品") || value.contains("product")) return "产品经理";
        if (value.contains("通用") || value.contains("general")) return "通用面试";
        if (value.contains("其他") || value.contains("other")) return "其他岗位";
        return role.trim();
    }

    public static String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) return "";
        return switch (difficulty.trim().toUpperCase(Locale.ROOT)) {
            case "BASIC", "EASY", "基础", "简单" -> "EASY";
            case "INTERMEDIATE", "MEDIUM", "中等" -> "MEDIUM";
            case "ADVANCED", "HARD", "困难" -> "HARD";
            default -> difficulty.trim();
        };
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
    public static String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
    private record Scored(Entry entry, double score) {}
}
