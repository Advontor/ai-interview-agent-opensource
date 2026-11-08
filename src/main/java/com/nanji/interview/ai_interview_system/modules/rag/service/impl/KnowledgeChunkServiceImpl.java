package com.nanji.interview.ai_interview_system.modules.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;
import com.nanji.interview.ai_interview_system.modules.rag.mapper.KnowledgeChunkMapper;
import com.nanji.interview.ai_interview_system.modules.rag.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * RAG知识库分块 Service 实现
 */
@Service
public class KnowledgeChunkServiceImpl extends ServiceImpl<KnowledgeChunkMapper, KnowledgeChunk> implements KnowledgeChunkService {

    /** 命中难度字段的正则前缀（题库内容形如 “难度: intermediate”） */
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("难度[:：]\\s*([A-Za-z]+)");

    /** 单个关键词最长长度，避免超长串拖慢 LIKE 查询 */
    private static final int MAX_KEYWORD_LENGTH = 24;

    @Override
    public List<KnowledgeChunk> listByPosition(String position) {
        return baseMapper.selectByPosition(position);
    }

    @Override
    public List<KnowledgeChunk> listByDocTitle(String docTitle) {
        return baseMapper.selectByDocTitle(docTitle);
    }

    @Override
    public List<KnowledgeChunk> searchByKeyword(String keyword) {
        return baseMapper.searchByKeyword(keyword);
    }

    @Override
    public List<String> listDocTitles() {
        return baseMapper.selectDistinctDocTitles();
    }

    @Override
    public boolean deleteByDocTitle(String docTitle) {
        return baseMapper.deleteByDocTitle(docTitle) > 0;
    }

    @Override
    public List<KnowledgeChunk> retrieveForInterview(String docTitle, List<String> keywords,
                                                    String difficulty, int limit) {
        if (docTitle == null || docTitle.isBlank() || limit <= 0) {
            return List.of();
        }
        List<String> cleanKeywords = sanitizeKeywords(keywords);
        // 多取一些候选，便于按难度优先挑选
        int probeSize = Math.min(Math.max(limit * 3, limit + 10), 60);

        List<KnowledgeChunk> candidates = baseMapper.selectForInterview(docTitle, cleanKeywords, probeSize);
        if (candidates.isEmpty() && !cleanKeywords.isEmpty()) {
            // 关键词全部落空：退化为按序取样，保证内容仍来自该题库
            candidates = baseMapper.selectForInterview(docTitle, null, probeSize);
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        // 同难度优先，其余作为补充
        List<KnowledgeChunk> preferred = new ArrayList<>();
        List<KnowledgeChunk> others = new ArrayList<>();
        for (KnowledgeChunk chunk : candidates) {
            if (matchesDifficulty(chunk.getContent(), difficulty)) {
                preferred.add(chunk);
            } else {
                others.add(chunk);
            }
        }
        preferred.addAll(others);
        return preferred.size() > limit ? new ArrayList<>(preferred.subList(0, limit)) : preferred;
    }

    /** 清洗关键词：去空白、去 LIKE 通配符、去重、限长限量 */
    private List<String> sanitizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : keywords) {
            if (raw == null) {
                continue;
            }
            String kw = raw.trim()
                    .replace("%", "")
                    .replace("_", "")
                    .replace("\\", "");
            if (kw.length() >= 2) {
                unique.add(kw.length() > MAX_KEYWORD_LENGTH ? kw.substring(0, MAX_KEYWORD_LENGTH) : kw);
            }
            if (unique.size() >= 10) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    /** 判断题库条目难度是否与面试难度一致（EASY→basic / MEDIUM→intermediate / HARD→advanced） */
    private boolean matchesDifficulty(String content, String difficulty) {
        if (content == null || difficulty == null || difficulty.isBlank()) {
            return false;
        }
        String target = switch (difficulty.trim().toUpperCase(Locale.ROOT)) {
            case "EASY" -> "basic";
            case "HARD" -> "advanced";
            default -> "intermediate";
        };
        java.util.regex.Matcher matcher = DIFFICULTY_PATTERN.matcher(content);
        while (matcher.find()) {
            if (target.equalsIgnoreCase(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }
}
