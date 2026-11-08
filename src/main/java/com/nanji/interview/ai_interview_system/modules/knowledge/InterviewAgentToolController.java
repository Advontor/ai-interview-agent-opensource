package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.nanji.interview.ai_interview_system.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** HTTP inspection endpoint for the Agent's deterministic local tool. */
@RestController
@RequestMapping("/agent/tools")
@Validated
public class InterviewAgentToolController {
    private final InterviewKnowledgeTool knowledgeTool;

    public InterviewAgentToolController(InterviewKnowledgeTool knowledgeTool) {
        this.knowledgeTool = knowledgeTool;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> definitions() {
        return Result.success(List.of(knowledgeTool.definition()));
    }

    @GetMapping("/search-interview-knowledge")
    public Result<InterviewKnowledgeTool.ToolResult> search(
            @RequestParam(value = "targetRole", defaultValue = "") @Size(max = 50) String targetRole,
            @RequestParam(value = "difficulty", defaultValue = "") @Size(max = 20) String difficulty,
            @RequestParam("query") @NotBlank @Size(max = 500) String query,
            @RequestParam(value = "topK", defaultValue = "3") @Min(1) @Max(5) int topK) {
        return Result.success(knowledgeTool.invoke(targetRole, difficulty, query, topK));
    }
}
