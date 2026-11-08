package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.nanji.interview.ai_interview_system.common.Result;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Testable HTTP entry point for the CSV knowledge base. */
@RestController
@RequestMapping("/knowledge/interview-rag")
@Validated
public class CsvInterviewKnowledgeController {
    private final CsvInterviewKnowledgeService service;

    public CsvInterviewKnowledgeController(CsvInterviewKnowledgeService service) { this.service = service; }

    @GetMapping("/stats")
    public Result<CsvInterviewKnowledgeService.Stats> stats() {
        return Result.success(service.stats());
    }

    @GetMapping("/search")
    public Result<CsvInterviewKnowledgeService.SearchResult> search(
            @RequestParam(value = "targetRole", defaultValue = "") @Size(max = 50) String targetRole,
            @RequestParam(value = "difficulty", defaultValue = "") @Pattern(regexp = "|EASY|MEDIUM|HARD|basic|intermediate|advanced|基础|简单|中等|困难") String difficulty,
            @RequestParam("query") @NotBlank @Size(max = 500) String query,
            @RequestParam(value = "topK", defaultValue = "5") @Min(1) @Max(5) int topK) {
        return Result.success(service.search(targetRole, difficulty, query, topK));
    }

    /**
     * Answer-aware retrieval used by the interview side panel.  It has no
     * side effects and can safely be retried when an SSE request reconnects.
     */
    @GetMapping("/recommend")
    public Result<CsvInterviewKnowledgeService.RecommendationResult> recommend(
            @RequestParam(value = "targetRole", defaultValue = "") @Size(max = 50) String targetRole,
            @RequestParam(value = "difficulty", defaultValue = "") @Pattern(regexp = "|EASY|MEDIUM|HARD|basic|intermediate|advanced|基础|简单|中等|困难") String difficulty,
            @RequestParam(value = "question", defaultValue = "") @Size(max = 1500) String question,
            @RequestParam(value = "answer", defaultValue = "") @Size(max = 8000) String answer,
            @RequestParam(value = "weakTopics", defaultValue = "") @Size(max = 600) String weakTopics,
            @RequestParam(value = "topK", defaultValue = "3") @Min(1) @Max(5) int topK) {
        if ((question == null || question.isBlank()) && (answer == null || answer.isBlank()))
            throw new IllegalArgumentException("question 或 answer 至少提供一项");
        return Result.success(service.recommend(targetRole, difficulty, question, answer, weakTopics, topK));
    }
}
