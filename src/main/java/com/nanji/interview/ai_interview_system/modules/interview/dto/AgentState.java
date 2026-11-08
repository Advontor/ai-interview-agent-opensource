package com.nanji.interview.ai_interview_system.modules.interview.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 面试进行状态：持久化到 interview_session.agent_state_json，
 * 刷新页面/重新进入会话时从数据库恢复。
 */
@Data
public class AgentState {

    /** 有效问题数（RETRY 重新询问不重复计数） */
    private int effectiveQuestionCount = 0;

    /** 当前讨论的知识点 */
    private String currentTopic;

    /** 当前知识点已追问次数（0~2） */
    private int currentTopicFollowUpCount = 0;

    /** 同一问题连续无效回答次数 */
    private int consecutiveInvalidCount = 0;

    /** 当前问题是否已做过一次 PARTIAL 澄清追问 */
    private boolean clarifiedCurrentQuestion = false;

    /** 已问过的问题列表 */
    private List<String> askedQuestions = new ArrayList<>();

    /** 已覆盖知识点列表 */
    private List<String> coveredTopics = new ArrayList<>();

    /** 未掌握/未获得有效回答的知识点列表 */
    private List<String> weakTopics = new ArrayList<>();

    /** 最近问答的一句话摘要（由模型维护） */
    private String recentSummary;

    /**
     * 绑定的知识库文档标题（「基于知识库面试」模式）。
     * 为空表示普通面试；非空时每轮出题都会检索该题库作为参考。
     */
    private String knowledgeBase;

    /**
     * 待发的下一道题：createSession 时已用 LLM 生成好第一道正题，但要先让候选人做自我介绍，
     * 所以把它暂存起来；等用户提交自我介绍后直接取出返回（不额外消耗一次 LLM 调用）。
     * 取出后置空。
     */
    private String pendingQuestion;

    /**
     * 待发的下一道题对应的参考答案（与 pendingQuestion 配套，来自知识库检索）。
     */
    private String pendingReferenceAnswer;

    /**
     * 用户配置的题目总数（来自 InterviewInitRequest.questionCount）。
     * 后端用它作为硬上限（达到即强制 FINISH），替代之前不稳定的硬编码 15。
     * 兜底默认 10。
     */
    private Integer maxQuestionCount = 10;
}
