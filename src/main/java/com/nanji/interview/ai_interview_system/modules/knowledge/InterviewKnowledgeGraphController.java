package com.nanji.interview.ai_interview_system.modules.knowledge;

import com.nanji.interview.ai_interview_system.common.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge/graphs")
public class InterviewKnowledgeGraphController {
    private final InterviewKnowledgeGraphService graphService;
    public InterviewKnowledgeGraphController(InterviewKnowledgeGraphService graphService) { this.graphService = graphService; }
    @GetMapping("/{sessionId}")
    public Result<InterviewKnowledgeGraphService.GraphResult> get(@PathVariable Long sessionId) {
        return Result.success(graphService.build(sessionId));
    }
}
