package com.nanji.interview.ai_interview_system.modules.interview.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewInitRequest;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionListItem;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionResponse;
import com.nanji.interview.ai_interview_system.modules.interview.dto.SubmitAnswerRequest;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewMessage;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewMessageService;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewSessionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 面试模块统一接口（第一周主流程）：
 *  - POST /interview/session：创建面试（入库 + 生成第一题）
 *  - POST /interview/answer：提交回答并获得下一题（答满 5 题自动结束并生成评价）
 *  - POST /interview/finish：结束面试（生成评价并保存报告）
 *  - 查询/消息接口沿用 develop 原有能力
 *
 * 注：原 PUT /interview/session/end/{sessionId} 仅改状态，与 /interview/finish
 * 等价且无法生成评价，已由 finish 取代删除。
 */
@RestController
@RequestMapping("/interview")
public class InterviewController {

    @Resource
    private InterviewSessionService interviewSessionService;

    @Resource
    private InterviewMessageService interviewMessageService;

    // 创建面试会话（生成第一题）
    @PostMapping("/session")
    public Result<InterviewSessionResponse> createSession(@Valid @RequestBody InterviewInitRequest request) {
        return Result.success(interviewSessionService.createSession(request));
    }

    // 提交回答并获得下一题
    @PostMapping("/answer")
    public Result<InterviewSessionResponse> submitAnswer(@Valid @RequestBody SubmitAnswerRequest request) {
        return Result.success(interviewSessionService.submitAnswer(request));
    }

    // 结束面试（生成评价并保存报告）
    @PostMapping("/finish")
    public Result<InterviewEvaluation> finishInterview(@RequestParam("sessionId") Long sessionId) {
        return Result.success(interviewSessionService.finishInterview(sessionId));
    }

    // 从数据库恢复会话最新状态（刷新/重新进入会话时调用）
    @GetMapping("/session/{sessionId}")
    public Result<InterviewSessionResponse> getSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.getSessionResponse(sessionId));
    }

    // 查询用户所有面试会话（历史列表：含候选人姓名/年龄/城市/学历、题库绑定与报告得分）
    @GetMapping("/session/list/{userId}")
    public Result<List<InterviewSessionListItem>> getSessionList(@PathVariable Long userId) {
        return Result.success(interviewSessionService.listItemsByUserId(userId));
    }

    // 删除一条面试记录（连同该会话的问答消息与评估报告）
    @DeleteMapping("/session/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable Long sessionId) {
        return Result.success(interviewSessionService.deleteSession(sessionId));
    }

    // 保存一条聊天消息
    @PostMapping("/message")
    public Result<Boolean> saveMessage(@RequestBody InterviewMessage message) {
        return Result.success(interviewMessageService.save(message));
    }

    // 根据会话id查询全部对话记录
    @GetMapping("/message/list/{sessionId}")
    public Result<List<InterviewMessage>> getMessageList(@PathVariable Long sessionId) {
        return Result.success(interviewMessageService.listBySessionId(sessionId));
    }
}
