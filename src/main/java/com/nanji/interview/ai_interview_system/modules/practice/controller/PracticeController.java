package com.nanji.interview.ai_interview_system.modules.practice.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.practice.dto.PracticeGradeRequest;
import com.nanji.interview.ai_interview_system.modules.practice.dto.PracticeGradeResult;
import com.nanji.interview.ai_interview_system.modules.practice.service.PracticeGradingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 刷题练习接口：单题作答即时评分，无需简历、不创建面试会话。
 */
@RestController
@RequestMapping("/practice")
public class PracticeController {

    @Resource
    private PracticeGradingService practiceGradingService;

    /**
     * 批改单题：给出一道题库真题的作答，返回得分、点评与参考答案。
     */
    @PostMapping("/grade")
    public Result<PracticeGradeResult> grade(@RequestBody PracticeGradeRequest request) {
        return Result.success(practiceGradingService.grade(request.getChunkId(), request.getAnswer()));
    }
}
