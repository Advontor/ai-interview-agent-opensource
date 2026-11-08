package com.nanji.interview.ai_interview_system.modules.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewInitRequest;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionListItem;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionResponse;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewTurn;
import com.nanji.interview.ai_interview_system.modules.interview.dto.SubmitAnswerRequest;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewSession;

import java.util.List;

/**
 * 面试会话服务（在 develop 原有 CRUD 能力之上，叠加 Agent 面试编排）。
 * <p>
 * 会话与消息均持久化到 interview_session / interview_message，
 * 不使用内存 ConcurrentHashMap。
 */
public interface InterviewSessionService extends IService<InterviewSession> {

    /** 创建面试会话：入库 → DeepSeek 生成第一题 → AI 消息入库 */
    InterviewSessionResponse createSession(InterviewInitRequest request);

    /** 提交回答：USER 消息入库 → DeepSeek 分析并生成下一题 → AI 消息入库；答满自动结束 */
    InterviewSessionResponse submitAnswer(SubmitAnswerRequest request);

    /**
     * 流式提交回答：业务流程与 submitAnswer 完全一致，
     * 区别是模型生成「下一道题」时逐字回调 onQuestionToken（真流式，非打字机模拟），
     * 全部处理完成后回调 onComplete 给出最终结果（含结束标记与评估报告）。
     * <p>
     * 下列场景没有可流式的题目，onQuestionToken 不会被回调，仅在结束时回调 onComplete：
     * 自我介绍后的预生成首题、已达题数上限直接结束、本地预判命中（空回答 / 跳过等）。
     *
     * @param onQuestionToken 下一题的增量文本回调，可为 null
     * @param onComplete      最终结果回调
     */
    void submitAnswerStream(Long sessionId, String answer,
                            java.util.function.Consumer<String> onQuestionToken,
                            java.util.function.Consumer<InterviewSessionResponse> onComplete);

    /** 结束面试：DeepSeek 生成评价 → 保存 report → session 更新为已结束 */
    InterviewEvaluation finishInterview(Long sessionId);

    /** 根据会话 id 按时间顺序整理完整问答轮次（题目+回答配对） */
    List<InterviewTurn> getTurns(Long sessionId);

    /** 从数据库恢复会话最新状态（刷新/重新进入会话时调用） */
    InterviewSessionResponse getSessionResponse(Long sessionId);

    /** 根据用户 id 查询该用户所有面试会话 */
    List<InterviewSession> listByUserId(Long userId);

    /**
     * 历史面试列表：在会话基础上补齐候选人基础信息（姓名/年龄/城市/学历）、
     * 题库绑定与报告得分，按创建时间倒序（最新的在最前）。
     */
    List<InterviewSessionListItem> listItemsByUserId(Long userId);

    /**
     * 删除一场面试记录：连同该会话的问答消息与评估报告一并删除。
     *
     * @param sessionId 会话 id
     * @return 是否删除成功（会话不存在时返回 false）
     */
    boolean deleteSession(Long sessionId);
}
