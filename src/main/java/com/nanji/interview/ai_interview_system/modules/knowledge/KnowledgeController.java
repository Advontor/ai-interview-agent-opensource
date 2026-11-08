package com.nanji.interview.ai_interview_system.modules.knowledge;
import com.nanji.interview.ai_interview_system.common.Result;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/knowledge")
@Validated
public class KnowledgeController {
 private final KnowledgeService service;
 public KnowledgeController(KnowledgeService service){this.service=service;}
 @GetMapping("/stats")
 public Result<KnowledgeService.Stats> stats() {
  return Result.success(service.stats());
 }
 @GetMapping("/search")
 public Result<KnowledgeService.Context> search(@RequestParam("targetRole") @NotBlank @Size(max=100) String targetRole,
   @RequestParam(value="difficulty", defaultValue="") @Pattern(regexp="|EASY|MEDIUM|HARD") String difficulty,
   @RequestParam("query") @NotBlank @Size(max=1000) String query,@RequestParam(value="topK", defaultValue="3") @Min(1) @Max(5) int topK) {
  return Result.success(service.context(targetRole,difficulty,query,topK));
 }
}
