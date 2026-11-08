package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvInterviewKnowledgeServiceTest {
    private static CsvInterviewKnowledgeService service;

    @BeforeAll
    static void loadResource() {
        service = new CsvInterviewKnowledgeService(new ObjectMapper());
    }

    @Test void loadsAllCsvRows() { assertThat(service.size()).isEqualTo(20_000); }

    @Test void retrievesJavaBackendQuestions() {
        var hits = service.retrieve("后端开发", "MEDIUM", "Java 线程池", 5);
        assertThat(hits).isNotEmpty().allMatch(hit -> hit.role().equals("后端开发"));
        assertThat(hits.get(0).question()).contains("Java");
    }

    @Test void retrievesFrontendQuestions() {
        var hits = service.retrieve("前端开发", "MEDIUM", "React 状态管理", 5);
        assertThat(hits).isNotEmpty().allMatch(hit -> hit.role().equals("前端开发"));
    }

    @Test void retrievesGeneralInterviewQuestions() {
        var hits = service.retrieve("通用面试", "EASY", "项目沟通", 5);
        assertThat(hits).isNotEmpty().allMatch(hit -> hit.role().equals("通用面试"));
    }

    @Test void retrievesProductQuestions() {
        var hits = service.retrieve("产品经理", "MEDIUM", "用户需求分析", 5);
        assertThat(hits).isNotEmpty().allMatch(hit -> hit.role().equals("产品经理"));
    }

    @Test void retrievesOtherJobQuestions() {
        var hits = service.retrieve("其他岗位", "", "团队协作", 5);
        assertThat(hits).isNotEmpty().allMatch(hit -> hit.role().equals("其他岗位"));
    }

    @Test void mapsDifficultyAliases() {
        var basic = service.retrieve("后端开发", "basic", "Java", 5);
        var easy = service.retrieve("后端开发", "EASY", "Java", 5);
        assertThat(basic).isNotEmpty().allMatch(hit -> hit.difficulty().equals("EASY"));
        assertThat(basic).containsExactlyElementsOf(easy);
    }

    @Test void neverReturnsMoreThanFiveRows() {
        assertThat(service.retrieve("后端开发", "", "Java", 50)).hasSizeLessThanOrEqualTo(5);
    }

    @Test void returnsEmptyWhenBelowRelevanceThreshold() {
        assertThat(service.retrieve("后端开发", "", "量子火星咖啡机不存在词", 5)).isEmpty();
    }

    @Test void preservesRequiredMetadata() {
        var hit = service.retrieve("前端开发", "", "React", 1).get(0);
        assertThat(hit.rowId()).startsWith("miqa-");
        assertThat(hit.question()).isNotBlank();
        assertThat(hit.answer()).isNotBlank();
        assertThat(hit.tags()).isNotEmpty();
        assertThat(hit.role()).isEqualTo("前端开发");
        assertThat(hit.difficulty()).isIn("EASY", "MEDIUM", "HARD");
    }

    @Test void retrievalIsDeterministic() {
        List<CsvInterviewKnowledgeService.Hit> first = service.retrieve("后端开发", "HARD", "Redis 缓存", 5);
        List<CsvInterviewKnowledgeService.Hit> second = service.retrieve("后端开发", "HARD", "Redis 缓存", 5);
        assertThat(second).containsExactlyElementsOf(first);
    }

    @Test void contextIsBoundedAndContainsRowId() {
        var result = service.search("后端开发", "MEDIUM", "Redis 缓存一致性", 5);
        assertThat(result.context()).hasSizeLessThanOrEqualTo(3001);
        if (!result.hits().isEmpty()) assertThat(result.context()).contains(result.hits().get(0).rowId());
    }

    @Test void answerAwareRecommendationsIncludeReasonAndSource() {
        var result = service.recommend("后端开发", "MEDIUM", "Redis 缓存一致性如何保证？",
                "我会考虑过期时间和并发更新", "缓存一致性", 3);
        assertThat(result.query()).contains("Redis");
        assertThat(result.recommendations()).isNotEmpty();
        assertThat(result.recommendations()).allSatisfy(item -> {
            assertThat(item.resourceId()).startsWith("interview-rag:");
            assertThat(item.source()).contains("interview-rag-v1");
            assertThat(item.reason()).isNotBlank();
        });
    }
}
