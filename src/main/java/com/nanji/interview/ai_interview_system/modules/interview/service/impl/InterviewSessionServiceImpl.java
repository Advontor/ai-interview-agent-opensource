package com.nanji.interview.ai_interview_system.modules.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.Constants;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.common.util.JsonUtils;
import com.nanji.interview.ai_interview_system.infrastructure.llm.LlmClient;
import com.nanji.interview.ai_interview_system.modules.interview.dto.AgentState;
import com.nanji.interview.ai_interview_system.modules.interview.dto.AnswerAssessment;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewInitRequest;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionListItem;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionResponse;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewTurn;
import com.nanji.interview.ai_interview_system.modules.interview.dto.SubmitAnswerRequest;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewMessage;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewSession;
import com.nanji.interview.ai_interview_system.modules.interview.mapper.InterviewSessionMapper;
import com.nanji.interview.ai_interview_system.modules.interview.prompt.AnswerAssessmentPrompt;
import com.nanji.interview.ai_interview_system.modules.interview.prompt.InterviewEvaluationPrompt;
import com.nanji.interview.ai_interview_system.modules.interview.prompt.QuestionGenerationPrompt;
import com.nanji.interview.ai_interview_system.modules.interview.util.StreamingJsonFieldExtractor;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewMessageService;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewSessionService;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;
import com.nanji.interview.ai_interview_system.modules.rag.service.KnowledgeChunkService;
import com.nanji.interview.ai_interview_system.modules.report.service.ReportService;
import com.nanji.interview.ai_interview_system.modules.resume.dto.Education;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试会话服务实现：动态 Agent 面试编排。
 *
 * 每轮回答流程（每轮最多调用 DeepSeek 1 次）：
 *  USER 消息入库 → 本地预判（空/纯数字/SKIP 等）→ 需要时 DeepSeek 一次调用返回
 *  {verdict, action, topic, nextQuestion, hint, memoryUpdate} → Java 按硬规则执行。
 *
 * 硬规则：
 *  - 最低有效问题数 6（<8 禁止 FINISH）
 *  - 建议范围 8~12（Agent 自主决定是否结束）
 *  - 最大有效问题数 15（强制 FINISH）
 *  - 每个知识点：主问题 + 最多 2 次追问
 *  - RETRY 重新询问不增加有效问题数
 *  - Agent 状态持久化到 interview_session.agent_state_json
 */
@Service
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession>
        implements InterviewSessionService {

    /** 建议范围下界：达到后才允许 Agent 自主 FINISH（低于该值代码强制继续，含最低 6 题硬门槛） */
    private static final int SUGGEST_FINISH_MIN = 6;
    /** 默认题目上限（兜底用：用户没传 questionCount 时） */
    private static final int DEFAULT_MAX_QUESTIONS = 10;
    /** 题目上限的最大值（兜底用：用户传入过大值时） */
    private static final int HARD_MAX_QUESTIONS = 20;
    /** 每个知识点最多追问次数 */
    private static final int MAX_TOPIC_FOLLOW_UPS = 2;

    /** 最低最低面试题数（含自我介绍） */
    private static final int ABSOLUTE_MIN_QUESTIONS = 5;

    /**
     * 面试开场固定题目：让候选人先做自我介绍。
     * 作为预热消息单独入库，不计入 effectiveQuestionCount 也不显示在 turn 列表里。
     */
    private static final String SELF_INTRO_QUESTION =
            "请你先做个自我介绍吧～可以围绕这几个方面展开：1) 你的姓名、毕业院校与专业；2) 与应聘岗位最相关的 1~2 段经历（项目 / 实习 / 比赛）；3) 你为什么应聘这个岗位。自我介绍结束后，我会开始针对你的简历出技术题。";

    /** 「基于知识库面试」时注入提示词的参考题目条数 */
    private static final int MAX_REFERENCE_CHUNKS = 5;

    /** 单条参考题目注入提示词时的最大字符数（控制 token 开销） */
    private static final int MAX_REFERENCE_CHARS = 420;

    /** 参考答案落库时的最大字符数 */
    private static final int MAX_REFERENCE_ANSWER_CHARS = 500;

    /** 为某道题找参考答案时，从题库取多少条候选来比对 */
    private static final int REFERENCE_PROBE_SIZE = 12;

    /** 中文关键词片段里出现这些字就判为虚词、不作为检索词 */
    private static final Set<String> STOP_WORDS = Set.of(
            "如何", "什么", "怎么", "为什么", "哪些", "请你", "你会", "你能", "说说", "讲讲",
            "介绍", "一下", "具体", "结合", "如果", "假设", "现在", "谈谈", "了解", "知道",
            "认为", "觉得", "可以", "进行", "使用", "实现", "完成", "有没有", "是否");

    /** 单独出现的无意义双字片段（避免「你说的」这种被当关键词） */
    private static final Set<String> STOP_FRAGMENTS = Set.of(
            "你说", "说的", "你的", "他的", "这个", "那个", "这些", "那些", "一个", "一些",
            "什么", "如何", "怎么", "因为", "所以", "但是", "而且", "然后", "就是", "还是");

    /** 抽取题库条目中的「问题 / 参考回答」 */
    private static final Pattern CHUNK_QA_PATTERN = Pattern.compile(
            "问题[:：]\\s*(.*?)\\s*(?:\\r?\\n)+\\s*参考回答[:：]\\s*(.*)",
            Pattern.DOTALL);

    /** 从岗位名中抽取英文技术关键词，如 “Java后端开发” -> Java */
    private static final Pattern JOB_TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+#.\\-]{1,}");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final InterviewMessageService messageService;
    private final ReportService reportService;
    private final KnowledgeChunkService knowledgeChunkService;

    public InterviewSessionServiceImpl(LlmClient llmClient,
                                       ObjectMapper objectMapper,
                                       InterviewMessageService messageService,
                                       ReportService reportService,
                                       KnowledgeChunkService knowledgeChunkService) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.messageService = messageService;
        this.reportService = reportService;
        this.knowledgeChunkService = knowledgeChunkService;
    }

    @Override
    @Transactional
    public InterviewSessionResponse createSession(InterviewInitRequest request) {
        InterviewSession session = new InterviewSession();
        session.setJobTitle(request.getJobTitle());
        session.setDifficulty(request.getDifficulty());
        session.setStatus(Constants.SESSION_STATUS_IN_PROGRESS);
        session.setResumeJson(resumeToJson(request.getResume()));
        session.setCreateTime(LocalDateTime.now());
        // questionIndex 还没开始计正题，先放 0；下面 recordQuestion 会把它更新为 1
        session.setQuestionIndex(0);
        save(session);

        String knowledgeBase = blankToNull(request.getKnowledgeBase());
        int maxQuestionCount = resolveMaxQuestionCount(request.getQuestionCount());

        // 1. 先保存一道「自我介绍」开场白（不计为有效题目）
        String introQuestion = SELF_INTRO_QUESTION;
        saveMessage(session.getId(), Constants.SENDER_AI, introQuestion, null);

        // 2. 初始化 Agent 状态：自我介绍不计正题（effectiveQuestionCount=0）。
        //    第一道正题不再预生成——等用户做完自我介绍后实时流式生成，
        //    保证包括首题在内的所有 LLM 出题都有逐字输出效果。
        AgentState state = new AgentState();
        state.setEffectiveQuestionCount(0);
        state.setMaxQuestionCount(maxQuestionCount);
        state.setKnowledgeBase(knowledgeBase);
        session.setAgentStateJson(toJson(state));
        session.setQuestionIndex(0);
        updateById(session);

        // 3. 返回「自我介绍」作为用户看到的第一题
        return buildResponse(session, introQuestion, false, null, null, null, null, maxQuestionCount);
    }

    @Override
    @Transactional
    public InterviewSessionResponse submitAnswer(SubmitAnswerRequest request) {
        return doSubmitAnswer(request.getSessionId(), request.getAnswer(), null);
    }

    @Override
    public void submitAnswerStream(Long sessionId, String answer,
                                   Consumer<String> onQuestionToken,
                                   Consumer<InterviewSessionResponse> onComplete) {
        InterviewSessionResponse response = doSubmitAnswer(sessionId, answer, onQuestionToken);
        if (onComplete != null) {
            onComplete.accept(response);
        }
    }

    /**
     * 提交回答的共用实现。
     *
     * @param onQuestionToken 不为 null 时走流式：模型生成下一题的过程中逐字回调；
     *                        为 null 时走原有同步路径。
     */
    private InterviewSessionResponse doSubmitAnswer(Long sessionId, String rawAnswer,
                                                    Consumer<String> onQuestionToken) {
        InterviewSession session = getById(sessionId);
        if (session == null) {
            throw new BusinessException("面试会话不存在");
        }
        if (Constants.SESSION_STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException("面试已经结束");
        }

        String answer = rawAnswer == null ? "" : rawAnswer.trim();
        String currentQuestion = lastAiQuestion(session.getId());
        AgentState state = readState(session);

        // USER 消息入库（含无效回答，保留完整记录）
        saveMessage(session.getId(), Constants.SENDER_USER, answer);

        // 0a. 兼容旧会话：createSession 预生成的首题仍直接取出（旧数据不含流式，属正常）
        if (hasText(state.getPendingQuestion())) {
            String pending = state.getPendingQuestion();
            String pendingReference = state.getPendingReferenceAnswer();
            state.setPendingQuestion(null);
            state.setPendingReferenceAnswer(null);
            saveMessage(session.getId(), Constants.SENDER_AI, pending, pendingReference);
            state.setEffectiveQuestionCount(1);
            state.getAskedQuestions().add(pending);
            persistState(session, state);
            return buildResponse(session, pending, false, null, "VALID", "NEXT_TOPIC", null,
                    state.getMaxQuestionCount());
        }

        // 0b. 自我介绍阶段结束：实时生成第一道正题（流式时逐字推送，自我介绍不作质量判定）
        if (state.getEffectiveQuestionCount() == 0 && state.getAskedQuestions().isEmpty()) {
            String firstQuestion = generateFirstQuestion(session, state, onQuestionToken);
            recordQuestion(session, state, firstQuestion);
            return buildResponse(session, firstQuestion, false, null, "VALID", "NEXT_TOPIC", null,
                    state.getMaxQuestionCount());
        }

        // 1. 本地预判：纯数字 / 空 / 乱码 / 明确跳过，不花 DeepSeek
        String localVerdict = localPrecheck(answer);
        AnswerAssessment assessment;
        if (localVerdict != null) {
            assessment = new AnswerAssessment();
            assessment.setVerdict(localVerdict);
        } else {
            // 流式场景预检：已达题数上限就直接结束，不再调用模型。
            // 否则会出现「先流式推出一道题，紧接着又宣布面试结束」的割裂体验。
            if (onQuestionToken != null
                    && state.getEffectiveQuestionCount() >= resolveMaxQuestionCount(state.getMaxQuestionCount())) {
                return doFinishFlow(session, state);
            }
            // 2. DeepSeek 一次调用：判定 + 决策 + 下一题（流式时逐字回调下一题）
            assessment = assessWithLlm(session, state, currentQuestion, answer, onQuestionToken);
        }

        // 3. Java 按硬规则执行决策
        return executeDecision(session, state, assessment, currentQuestion, answer, onQuestionToken);
    }

    @Override
    @Transactional
    public InterviewEvaluation finishInterview(Long sessionId) {
        InterviewSession session = getById(sessionId);
        if (session == null) {
            throw new BusinessException("面试会话不存在");
        }
        if (Constants.SESSION_STATUS_FINISHED.equals(session.getStatus())) {
            return reportService.getEvaluationBySessionId(sessionId);
        }
        List<InterviewTurn> turns = getTurns(sessionId);
        if (turns.isEmpty()) {
            throw new BusinessException("当前没有可评价的面试回答");
        }
        return doFinish(session, turns);
    }

    @Override
    public InterviewSessionResponse getSessionResponse(Long sessionId) {
        InterviewSession session = getById(sessionId);
        if (session == null) {
            throw new BusinessException("面试会话不存在");
        }
        boolean finished = Constants.SESSION_STATUS_FINISHED.equals(session.getStatus());
        InterviewEvaluation evaluation = null;
        if (finished) {
            try {
                evaluation = reportService.getEvaluationBySessionId(sessionId);
            } catch (BusinessException ignored) {
                // 尚未生成报告
            }
        }
        AgentState state = readState(session);
        int count = state.getEffectiveQuestionCount() > 0
                ? state.getEffectiveQuestionCount()
                : (session.getQuestionIndex() == null ? 1 : session.getQuestionIndex());
        return buildResponse(session, finished ? null : lastAiQuestion(sessionId),
                finished, evaluation, null, null, String.valueOf(count),
                state.getMaxQuestionCount());
    }

    @Override
    public List<InterviewTurn> getTurns(Long sessionId) {
        List<InterviewMessage> messages = messageService.listBySessionId(sessionId);
        List<InterviewTurn> turns = new ArrayList<>();
        String pendingQuestion = null;
        String pendingReferenceAnswer = null;
        String pendingAnswer = null;
        for (InterviewMessage message : messages) {
            if (Constants.SENDER_AI.equals(message.getSender())) {
                // 自我介绍这种「预热」消息没有对应 USER 回答，跳过不计为 turn
                if (SELF_INTRO_QUESTION.equals(message.getContent())) {
                    pendingQuestion = null;
                    pendingReferenceAnswer = null;
                    pendingAnswer = null;
                    continue;
                }
                if (pendingQuestion != null && pendingAnswer != null) {
                    turns.add(new InterviewTurn(pendingQuestion, pendingAnswer, pendingReferenceAnswer));
                    pendingAnswer = null;
                    pendingReferenceAnswer = null;
                }
                pendingQuestion = message.getContent();
                pendingReferenceAnswer = message.getReferenceAnswer();
            } else if (Constants.SENDER_USER.equals(message.getSender())) {
                // 同一问题被 RETRY 重答时，以最后一次回答为准
                pendingAnswer = message.getContent();
            }
        }
        if (pendingQuestion != null && pendingAnswer != null) {
            turns.add(new InterviewTurn(pendingQuestion, pendingAnswer, pendingReferenceAnswer));
        }
        return turns;
    }

    @Override
    public List<InterviewSession> listByUserId(Long userId) {
        // 最新的排在最前，便于历史列表直接展示
        return lambdaQuery()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getId)
                .list();
    }

    @Override
    public List<InterviewSessionListItem> listItemsByUserId(Long userId) {
        List<InterviewSession> sessions = listByUserId(userId);
        if (sessions.isEmpty()) {
            return List.of();
        }

        // 一次性把报告得分查出来，避免逐条查询
        List<Long> sessionIds = new ArrayList<>(sessions.size());
        for (InterviewSession session : sessions) {
            sessionIds.add(session.getId());
        }
        Map<Long, Integer> scores = reportService.findScoresBySessionIds(sessionIds);

        List<InterviewSessionListItem> items = new ArrayList<>(sessions.size());
        for (InterviewSession session : sessions) {
            InterviewSessionListItem item = new InterviewSessionListItem();
            item.setId(session.getId());
            item.setJobTitle(session.getJobTitle());
            item.setDifficulty(session.getDifficulty());
            item.setStatus(session.getStatus());
            item.setQuestionCount(session.getQuestionIndex());
            item.setCreateTime(session.getCreateTime());
            item.setEndTime(session.getEndTime());
            item.setScore(scores.get(session.getId()));

            // 从简历 JSON 中取出列表页需要展示的基础信息
            ResumeDTO resume = readResume(session);
            if (resume != null && resume.getBasicInfo() != null) {
                item.setCandidateName(blankToNull(resume.getBasicInfo().getName()));
                item.setCandidateAge(blankToNull(resume.getBasicInfo().getAge()));
                item.setCandidateCity(blankToNull(resume.getBasicInfo().getCity()));
            }
            item.setCandidateEducation(firstDegree(resume));
            item.setKnowledgeBase(readState(session).getKnowledgeBase());

            items.add(item);
        }
        return items;
    }

    /** 取第一段填写了学历的教育经历作为最高学历展示 */
    private String firstDegree(ResumeDTO resume) {
        if (resume == null || resume.getEducationList() == null) {
            return null;
        }
        for (Education education : resume.getEducationList()) {
            if (education != null && hasText(education.getDegree())) {
                return education.getDegree().trim();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public boolean deleteSession(Long sessionId) {
        if (sessionId == null) {
            return false;
        }
        InterviewSession session = getById(sessionId);
        if (session == null) {
            return false;
        }
        // 连同该会话的问答消息与评估报告一并清理，避免留下孤儿数据
        messageService.remove(new LambdaQueryWrapper<InterviewMessage>()
                .eq(InterviewMessage::getSessionId, sessionId));
        reportService.deleteBySessionId(sessionId);
        return removeById(sessionId);
    }

    // ---------------- 决策执行 ----------------

    private InterviewSessionResponse executeDecision(InterviewSession session, AgentState state,
                                                     AnswerAssessment assessment, String currentQuestion,
                                                     String rawAnswer, Consumer<String> sink) {
        // 硬上限前置检查：无论本轮判定是 VALID / PARTIAL / SKIP / INVALID，只要已达用户配置题数就强制结束。
        // （之前只在 VALID 分支检查，SKIP / INVALID 走 askNextTopic 会绕过上限，导致「设 10 题问出 12/18 题」）
        int maxQuestions = resolveMaxQuestionCount(state.getMaxQuestionCount());
        if (state.getEffectiveQuestionCount() >= maxQuestions) {
            return doFinishFlow(session, state);
        }

        String verdict = assessment.getVerdict() == null ? "VALID" : assessment.getVerdict();

        // LLM 兜底纠偏：本地判定「明显不是无效回答」时，拒绝 LLM 的 INVALID 误判（改为 VALID）。
        // 背景：线上实测同一段 60 字技术回答会被随机判 INVALID，导致 RETRY / 多切题，题数虚高。
        if ("INVALID".equals(verdict) && isSubstantiveAnswer(rawAnswer)) {
            verdict = "VALID";
            assessment.setVerdict("VALID");
        }

        if ("INVALID".equals(verdict)) {
            state.setConsecutiveInvalidCount(state.getConsecutiveInvalidCount() + 1);
            if (state.getConsecutiveInvalidCount() >= 2) {
                // 连续第二次无效：记录未获得有效回答，切换知识点
                markWeakTopic(state, state.getCurrentTopic());
                state.setConsecutiveInvalidCount(0);
                return askNextTopic(session, state, currentQuestion, verdict, assessment, sink);
            }
            // 第一次无效：RETRY，不生成新题，不增加有效问题数
            persistState(session, state);
            return buildResponse(session, currentQuestion, false, null,
                    "INVALID", "RETRY", "你的回答似乎与问题无关，请认真重新回答当前问题。",
                    state.getMaxQuestionCount());
        }

        state.setConsecutiveInvalidCount(0);

        if ("SKIP".equals(verdict)) {
            markWeakTopic(state, state.getCurrentTopic());
            return askNextTopic(session, state, currentQuestion, verdict, assessment, sink);
        }

        if ("PARTIAL".equals(verdict)) {
            if (!state.isClarifiedCurrentQuestion()) {
                state.setClarifiedCurrentQuestion(true);
                return askFollowUp(session, state, assessment, sink);
            }
            return askNextTopic(session, state, currentQuestion, verdict, assessment, sink);
        }

        // VALID：按硬规则修正 Agent 决策（题数上限已在方法开头统一拦截）
        String action = assessment.getAction();
        if ("FOLLOW_UP".equals(action)
                && state.getCurrentTopicFollowUpCount() >= MAX_TOPIC_FOLLOW_UPS) {
            action = "NEXT_TOPIC";
        }
        // 最低门槛：与用户配置联动，避免「用户设 5 题但硬门槛 6 题」导致永远结束不了
        int minRequired = Math.min(SUGGEST_FINISH_MIN, maxQuestions);
        if ("FINISH".equals(action) && state.getEffectiveQuestionCount() < minRequired) {
            action = "NEXT_TOPIC";
        }

        switch (action == null ? "NEXT_TOPIC" : action) {
            case "FINISH":
                return doFinishFlow(session, state);
            case "FOLLOW_UP":
                return askFollowUp(session, state, assessment, sink);
            default:
                return askNextTopic(session, state, currentQuestion, assessment.getVerdict(), assessment, sink);
        }
    }

    /** 生成追问：同一知识点，追问次数 +1，计入有效问题数 */
    private InterviewSessionResponse askFollowUp(InterviewSession session, AgentState state,
                                                 AnswerAssessment assessment, Consumer<String> sink) {
        String question = nextQuestionOrFallback(session, state, assessment, sink);
        state.setCurrentTopicFollowUpCount(state.getCurrentTopicFollowUpCount() + 1);
        if (hasText(assessment.getTopic())) {
            state.setCurrentTopic(assessment.getTopic().trim());
        }
        recordQuestion(session, state, question);
        return buildResponse(session, question, false, null,
                assessment.getVerdict(), "FOLLOW_UP", null, state.getMaxQuestionCount());
    }

    /**
     * 切换知识点：追问计数清零，计入有效问题数。
     *
     * @param assessment 本轮模型判定结果；不为 null 且带 nextQuestion 时直接沿用，
     *                   避免丢弃模型已生成的题目再调一次 LLM
     *                   （此前固定传 null，导致「换知识点」时题目被重新生成：
     *                   既多花一次调用，也让流式推出的题目与最终落库的题目不一致）。
     */
    private InterviewSessionResponse askNextTopic(InterviewSession session, AgentState state,
                                                  String currentQuestion, String verdict,
                                                  AnswerAssessment assessment, Consumer<String> sink) {
        if (state.getCurrentTopic() != null && hasText(state.getCurrentTopic())) {
            if (!state.getCoveredTopics().contains(state.getCurrentTopic())
                    && !state.getWeakTopics().contains(state.getCurrentTopic())) {
                state.getCoveredTopics().add(state.getCurrentTopic());
            }
        }
        String question = nextQuestionOrFallback(session, state, assessment, sink);
        state.setCurrentTopicFollowUpCount(0);
        state.setClarifiedCurrentQuestion(false);
        recordQuestion(session, state, question);
        return buildResponse(session, question, false, null,
                verdict, "NEXT_TOPIC", null, state.getMaxQuestionCount());
    }

    private InterviewSessionResponse doFinishFlow(InterviewSession session, AgentState state) {
        persistState(session, state);
        List<InterviewTurn> turns = getTurns(session.getId());
        if (turns.isEmpty()) {
            throw new BusinessException("当前没有可评价的面试回答");
        }
        InterviewEvaluation evaluation = doFinish(session, turns);
        return buildResponse(session, null, true, evaluation, "VALID", "FINISH", null, state.getMaxQuestionCount());
    }

    /** 结束面试：生成评价 → 保存 report → session 更新为已结束 */
    private InterviewEvaluation doFinish(InterviewSession session, List<InterviewTurn> turns) {
        InterviewEvaluation evaluation = generateEvaluation(session, turns);
        reportService.saveEvaluation(session.getId(), evaluation);
        session.setStatus(Constants.SESSION_STATUS_FINISHED);
        session.setEndTime(LocalDateTime.now());
        updateById(session);
        return evaluation;
    }

    /**
     * 优先使用模型返回的下一题；为空时兜底调用模型按“切换知识点”生成（保证每轮最多 1 次调用）。
     *
     * @param sink 不为 null 时，兜底生成的题目也逐字回调出去
     *             （跳过 / 无效回答等走本地预判的轮次同样能看到逐字效果）
     */
    private String nextQuestionOrFallback(InterviewSession session, AgentState state,
                                          AnswerAssessment assessment, Consumer<String> sink) {
        if (assessment != null && hasText(assessment.getNextQuestion())) {
            return assessment.getNextQuestion().trim();
        }
        String userPrompt = buildContextPrompt(session, state);
        String systemPrompt = AnswerAssessmentPrompt.buildNextTopicSystemPrompt();
        String question;
        if (sink == null) {
            question = llmClient.chat(systemPrompt, userPrompt);
        } else {
            StringBuilder acc = new StringBuilder();
            llmClient.chatStream(systemPrompt, userPrompt, chunk -> {
                acc.append(chunk);
                sink.accept(chunk);
            });
            question = acc.toString();
        }
        if (!hasText(question)) {
            question = "关于" + nullToEmpty(state.getCurrentTopic()) + "，你还有哪些更深入的理解？";
        }
        return question.trim();
    }

    /**
     * DeepSeek 一次调用：回答判定 + 决策 + 下一题。
     *
     * @param onQuestionToken 不为 null 时走流式：边接收模型增量边把 nextQuestion 的值逐字吐出，
     *                        JSON 全部接收完后再统一解析出判定结果。
     */
    private AnswerAssessment assessWithLlm(InterviewSession session, AgentState state,
                                           String currentQuestion, String answer,
                                           Consumer<String> onQuestionToken) {
        String systemPrompt = AnswerAssessmentPrompt.buildSystemPrompt();
        String userPrompt = buildContextPrompt(session, state) + """

                当前问题：
                %s

                候选人本轮回答：
                %s

                请给出判定、决策与（如需要）下一道面试题。
                """.formatted(nullToEmpty(currentQuestion), answer);

        String json;
        StreamingJsonFieldExtractor extractor = null;
        if (onQuestionToken == null) {
            json = llmClient.chat(systemPrompt, userPrompt);
        } else {
            extractor = new StreamingJsonFieldExtractor("nextQuestion", onQuestionToken);
            llmClient.chatStream(systemPrompt, userPrompt, extractor::push);
            json = extractor.getBufferText();
        }
        try {
            return objectMapper.readValue(JsonUtils.cleanCodeFence(json), AnswerAssessment.class);
        } catch (Exception e) {
            // 流式场景下的补救：题目已经逐字推给前端了，此时若抛异常让上层降级重新出题，
            // 用户会看到「题目显示到一半突然变成另一道」。
            // 只要题目已完整取出，就用它构造一个最小判定结果继续走决策，避免二次调用。
            if (extractor != null && extractor.isClosed() && hasText(extractor.getValue())) {
                System.err.println("[WARN] 判定 JSON 解析失败，已用流式取出的题目兜底；原始输出："
                        + (json == null ? "null" : json.substring(0, Math.min(500, json.length()))));
                AnswerAssessment salvaged = new AnswerAssessment();
                salvaged.setVerdict("VALID");
                salvaged.setAction("NEXT_TOPIC");
                salvaged.setNextQuestion(extractor.getValue().trim());
                return salvaged;
            }
            throw new BusinessException("回答分析结果解析失败", e);
        }
    }

    /** 判定 + 决策所需的完整上下文 */
    private String buildContextPrompt(InterviewSession session, AgentState state) {
        String knowledgeReference = buildKnowledgeReference(
                state.getKnowledgeBase(), session.getJobTitle(), readResume(session), session.getDifficulty());
        return """
                应聘岗位：%s
                面试难度：%s
                候选人简历（JSON）：%s
                %s
                当前有效问题数：%d
                当前知识点：%s
                当前知识点已追问次数：%d
                已问过的问题：%s
                已覆盖知识点：%s
                未掌握知识点：%s
                最近摘要：%s

                最近问答记录：
                %s
                """.formatted(
                session.getJobTitle(),
                session.getDifficulty(),
                resumeText(session),
                knowledgeReference,
                state.getEffectiveQuestionCount(),
                nullToEmpty(state.getCurrentTopic()),
                state.getCurrentTopicFollowUpCount(),
                state.getAskedQuestions(),
                state.getCoveredTopics(),
                state.getWeakTopics(),
                nullToEmpty(state.getRecentSummary()),
                recentTurnsText(session.getId())
        );
    }

    // ---------------- 「基于知识库面试」检索 ----------------

    /**
     * 构造注入提示词的【参考题库】文本块。
     * 未绑定知识库、题库不存在或检索为空时返回空串（退化为普通面试，不影响原有流程）。
     */
    private String buildKnowledgeReference(String knowledgeBase, String jobTitle, ResumeDTO resume, String difficulty) {
        if (knowledgeBase == null || knowledgeBase.isBlank()) {
            return "";
        }
        List<KnowledgeChunk> chunks = knowledgeChunkService.retrieveForInterview(
                knowledgeBase,
                extractKnowledgeKeywords(jobTitle, resume),
                difficulty,
                MAX_REFERENCE_CHUNKS
        );
        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("【参考题库】来源：知识库「").append(knowledgeBase).append("」，共检索到 ")
                .append(chunks.size()).append(" 道真题：").append(System.lineSeparator());
        int index = 1;
        for (KnowledgeChunk chunk : chunks) {
            String question = null;
            String answer = null;
            Matcher matcher = CHUNK_QA_PATTERN.matcher(nullToEmpty(chunk.getContent()));
            if (matcher.find()) {
                question = matcher.group(1).trim();
                answer = matcher.group(2).trim();
            } else {
                question = nullToEmpty(chunk.getContent()).trim();
            }
            builder.append(index++).append(". 问题：").append(truncate(question, MAX_REFERENCE_CHARS))
                    .append(System.lineSeparator());
            if (hasText(answer)) {
                builder.append("   参考答案要点：").append(truncate(answer, MAX_REFERENCE_CHARS))
                        .append(System.lineSeparator());
            }
        }
        builder.append("【出题要求】请优先围绕上述题库涉及的考点与难度风格出题：可以更换问法或结合候选人简历，")
                .append("但不要照抄原题；若与简历事实冲突，以简历为准。")
                .append(System.lineSeparator());
        return builder.toString();
    }

    /** 检索关键词：应聘岗位中的英文技术词 + 简历目标岗位 + 简历技能标签 */
    private List<String> extractKnowledgeKeywords(String jobTitle, ResumeDTO resume) {
        Set<String> keywords = new LinkedHashSet<>();
        addTokenKeywords(keywords, jobTitle);
        if (resume != null) {
            addTokenKeywords(keywords, resume.getTargetRole());
            if (resume.getSkills() != null) {
                for (String skill : resume.getSkills()) {
                    if (skill != null && !skill.isBlank()) {
                        keywords.add(skill.trim());
                    }
                }
            }
        }
        return new ArrayList<>(keywords);
    }

    private void addTokenKeywords(Set<String> keywords, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = JOB_TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }
    }

    /** 从会话快照中还原简历结构（失败时返回 null，退化为无关键词检索） */
    private ResumeDTO readResume(InterviewSession session) {
        String json = session.getResumeJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ResumeDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 记录新题：入消息表 + 更新状态。同步检索知识库填入参考答案（仅「基于知识库面试」生效） */
    private void recordQuestion(InterviewSession session, AgentState state, String question) {
        String reference = findReferenceAnswer(state.getKnowledgeBase(), question,
                session.getJobTitle(), readResume(session), session.getDifficulty());
        saveMessage(session.getId(), Constants.SENDER_AI, question, reference);
        state.setEffectiveQuestionCount(state.getEffectiveQuestionCount() + 1);
        state.getAskedQuestions().add(question);
        persistState(session, state);
    }

    /**
     * 生成第一道正题（自我介绍结束后调用）。
     * sink 不为 null 时走流式：模型每吐一段就逐字回调，前端即见打字机效果。
     */
    private String generateFirstQuestion(InterviewSession session, AgentState state,
                                         Consumer<String> sink) {
        String knowledgeReference = buildKnowledgeReference(
                state.getKnowledgeBase(), session.getJobTitle(), readResume(session), session.getDifficulty());
        String userPrompt = """
                应聘岗位：%s
                面试难度：%s
                候选人简历（JSON）：%s
                %s
                候选人刚做完自我介绍。请生成第一道面试题。
                """.formatted(
                session.getJobTitle(),
                session.getDifficulty(),
                session.getResumeJson(),
                knowledgeReference
        );
        String systemPrompt = QuestionGenerationPrompt.buildSystemPrompt();

        String question;
        if (sink == null) {
            question = llmClient.chat(systemPrompt, userPrompt);
        } else {
            StringBuilder collected = new StringBuilder();
            llmClient.chatStream(systemPrompt, userPrompt, token -> {
                collected.append(token);
                sink.accept(token);
            });
            question = collected.toString();
        }
        question = question == null ? "" : question.trim();
        if (!hasText(question)) {
            question = "请结合你简历中的项目经历，挑一个你最有亮点的项目，讲讲你负责的模块和遇到的最大技术挑战。";
        }
        return question;
    }

    private void persistState(InterviewSession session, AgentState state) {
        session.setAgentStateJson(toJson(state));
        session.setQuestionIndex(state.getEffectiveQuestionCount());
        updateById(session);
    }

    private AgentState readState(InterviewSession session) {
        if (session.getAgentStateJson() == null || session.getAgentStateJson().isBlank()) {
            return new AgentState();
        }
        try {
            AgentState state = objectMapper.readValue(session.getAgentStateJson(), AgentState.class);
            return state == null ? new AgentState() : state;
        } catch (Exception e) {
            return new AgentState();
        }
    }

    /** 回答达到该长度即视为「认真作答」，LLM 若误判 INVALID 则强制纠正为 VALID，避免题数虚高 */
    private static final int SUBSTANTIVE_ANSWER_MIN_LENGTH = 40;

    /** 本地快速预判，返回 null 表示需要 LLM 判断 */
    private String localPrecheck(String answer) {
        if (answer == null || answer.isBlank()) {
            return "INVALID";
        }
        // 前端「跳过本题」按钮送的固定标记，最先判定（长度不定，单独判断）
        if (answer.contains("用户跳过本题")) {
            return "SKIP";
        }
        // 明确表示不知道 / 不会 / 跳过
        if (answer.length() <= 30) {
            for (String word : new String[]{"不知道", "不会", "不懂", "没学过", "没接触过", "不清楚",
                    "跳过", "skip", "pass"}) {
                if (answer.toLowerCase().contains(word)) {
                    return "SKIP";
                }
            }
        }
        // 纯数字
        if (answer.matches("\\d+")) {
            return "INVALID";
        }
        // 纯标点 / 重复字符 / 单个字符
        if (answer.matches("^\\p{Punct}+$") || answer.matches("^(.)\\1+$") || answer.length() == 1) {
            return "INVALID";
        }
        return null;
    }

    /**
     * 是否为「认真作答」的长回答：达到一定长度、且不命中本地已能识别的无效/跳过特征。
     * 用于拒绝 LLM 对正常技术回答的 INVALID 误判。
     */
    private boolean isSubstantiveAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String text = answer.trim();
        if (text.length() < SUBSTANTIVE_ANSWER_MIN_LENGTH) {
            return false;
        }
        // 本地已能确定为无效 / 跳过的，不参与纠偏
        if (text.matches("\\d+") || text.matches("^\\p{Punct}+$") || text.matches("^(.)\\1+$")) {
            return false;
        }
        return true;
    }

    private void markWeakTopic(AgentState state, String topic) {
        if (hasText(topic) && !state.getWeakTopics().contains(topic)) {
            state.getWeakTopics().add(topic);
        }
        state.setCurrentTopicFollowUpCount(0);
        state.setClarifiedCurrentQuestion(false);
    }

    // ---------------- 评价 ----------------

    private InterviewEvaluation generateEvaluation(InterviewSession session, List<InterviewTurn> turns) {
        String userPrompt = """
                应聘岗位：%s
                面试难度：%s
                候选人简历（JSON）：%s

                完整面试问答记录：

                %s

                请根据以上信息生成本次面试的基础评价。
                """.formatted(
                session.getJobTitle(),
                session.getDifficulty(),
                resumeText(session),
                turnsText(turns)
        );

        String evaluationJson = llmClient.chat(
                InterviewEvaluationPrompt.buildSystemPrompt(),
                userPrompt
        );

        InterviewEvaluation evaluation;
        try {
            evaluation = objectMapper.readValue(
                    JsonUtils.cleanCodeFence(evaluationJson),
                    InterviewEvaluation.class
            );
        } catch (Exception e) {
            throw new BusinessException("面试评价解析失败", e);
        }

        // 岗位与难度随报告落库，报告页需要展示
        evaluation.setJobTitle(session.getJobTitle());
        evaluation.setDifficulty(session.getDifficulty());
        // 逐题评价：模型只给出 score/comment，题目与回答原文由后端按 index 回填，
        // 保证报告页展示的题目顺序与问答记录严格一致（且天然不含开场自我介绍）
        evaluation.setQuestionReviews(mergeQuestionReviews(evaluation.getQuestionReviews(), turns));
        return evaluation;
    }

    /**
     * 把模型返回的逐题评分/点评与真实问答记录对齐：
     * 以问答记录（turns）为基准，index 从 1 开始顺序对应；
     * 模型漏给或 index 越界时，该题得分缺省为 null（前端会显示“暂无点评”而非 0 分）。
     */
    private List<InterviewEvaluation.QuestionReview> mergeQuestionReviews(
            List<InterviewEvaluation.QuestionReview> reviews, List<InterviewTurn> turns) {
        Map<Integer, InterviewEvaluation.QuestionReview> byIndex = new HashMap<>();
        if (reviews != null) {
            for (InterviewEvaluation.QuestionReview review : reviews) {
                if (review != null && review.getIndex() > 0) {
                    byIndex.putIfAbsent(review.getIndex(), review);
                }
            }
        }

        List<InterviewEvaluation.QuestionReview> merged = new ArrayList<>();
        for (int i = 0; i < turns.size(); i++) {
            InterviewTurn turn = turns.get(i);
            InterviewEvaluation.QuestionReview review = byIndex.get(i + 1);
            InterviewEvaluation.QuestionReview item = new InterviewEvaluation.QuestionReview();
            item.setIndex(i + 1);
            item.setQuestion(turn.getQuestion());
            item.setAnswer(turn.getAnswer());
            item.setReferenceAnswer(turn.getReferenceAnswer());
            if (review != null) {
                item.setScore(normalizeScore(review.getScore()));
                item.setComment(blankToNull(review.getComment()));
            }
            merged.add(item);
        }
        return merged;
    }

    /** 把模型给出的分数收敛到 0~100；非数字或缺失返回 null（区别于真实的 0 分） */
    private Integer normalizeScore(Integer score) {
        if (score == null || score < 0) {
            return null;
        }
        return Math.min(score, 100);
    }

    // ---------------- 辅助 ----------------

    private void saveMessage(Long sessionId, String sender, String content) {
        saveMessage(sessionId, sender, content, null);
    }

    private void saveMessage(Long sessionId, String sender, String content, String referenceAnswer) {
        InterviewMessage message = new InterviewMessage();
        message.setSessionId(sessionId);
        message.setSender(sender);
        message.setContent(content);
        if (hasText(referenceAnswer)) {
            message.setReferenceAnswer(referenceAnswer);
        }
        message.setCreateTime(LocalDateTime.now());
        messageService.save(message);
    }

    /**
     * 检索知识库，从与当前题目最相关的 chunk 中提取参考答案。
     * 仅「基于知识库面试」模式生效；普通面试或检索失败时返回 null。
     * <p>
     * 匹配策略：
     * 1) 用题目里的技术关键词去题库检索（而非岗位关键词，命中更准）；
     * 2) 用「关键词命中数」给候选条目打分，取最高分；
     * 3) 命中数低于阈值直接返回 null —— 宁可不给参考，也不给八竿子打不着的答案。
     */
    private String findReferenceAnswer(String knowledgeBase, String question,
                                       String jobTitle, ResumeDTO resume, String difficulty) {
        if (!hasText(knowledgeBase) || !hasText(question)) {
            return null;
        }
        try {
            List<String> keywords = extractQuestionKeywords(question);
            if (keywords.isEmpty()) {
                // 题目里抠不出技术词时，退回岗位/技能关键词
                keywords = extractKnowledgeKeywords(jobTitle, resume);
            }
            if (keywords.isEmpty()) {
                return null;
            }
            List<KnowledgeChunk> chunks = knowledgeChunkService.retrieveForInterview(
                    knowledgeBase, keywords, difficulty, REFERENCE_PROBE_SIZE);
            if (chunks.isEmpty()) {
                return null;
            }

            KnowledgeChunk best = null;
            int bestScore = 0;
            for (KnowledgeChunk chunk : chunks) {
                String chunkQuestion = extractChunkQuestion(chunk.getContent());
                int score = scoreChunkMatch(chunkQuestion, keywords);
                if (score > bestScore) {
                    bestScore = score;
                    best = chunk;
                }
            }
            if (best == null) {
                // 题库里没找到有明显相关度的题，退化为取第一条同岗位同难度的真题，
                // 作为「这一类问题」的参考回答（前端已标注为"参考回复"）。
                best = chunks.get(0);
            }

            Matcher matcher = CHUNK_QA_PATTERN.matcher(nullToEmpty(best.getContent()));
            if (matcher.find()) {
                String answer = matcher.group(2).trim();
                return hasText(answer) ? truncate(answer, MAX_REFERENCE_ANSWER_CHARS) : null;
            }
            return null;
        } catch (Exception e) {
            // 检索失败不影响主流程
            return null;
        }
    }

    /** 取出题库条目的问题文本 */
    private String extractChunkQuestion(String content) {
        Matcher matcher = CHUNK_QA_PATTERN.matcher(nullToEmpty(content));
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return nullToEmpty(content);
    }

    /**
     * 给候选条目与当前题目的相关度打分：
     * 英文技术词命中权重高（说明技术栈对得上），中文片段命中权重低（易误命中通用词）。
     */
    private int scoreChunkMatch(String chunkQuestion, List<String> keywords) {
        if (!hasText(chunkQuestion)) {
            return 0;
        }
        int score = 0;
        for (String kw : keywords) {
            if (kw == null || kw.isBlank() || !chunkQuestion.contains(kw)) {
                continue;
            }
            score += kw.matches("[A-Za-z].*") ? 3 : 1;
        }
        return score;
    }

    /**
     * 从题目文本中抽取技术关键词：
     * 英文/数字技术词（Java、MySQL、Redis…）+ 中文 2~4 字技术片段（缓存、索引、并发…），
     * 过滤掉「如何 / 请你 / 为什么 / 你会 / 说一下」这类无意义虚词。
     */
    private List<String> extractQuestionKeywords(String question) {
        List<String> keywords = new ArrayList<>();
        if (!hasText(question)) {
            return keywords;
        }
        // 1. 英文技术词
        Matcher matcher = JOB_TOKEN_PATTERN.matcher(question);
        while (matcher.find() && keywords.size() < 8) {
            String token = matcher.group();
            if (!keywords.contains(token)) {
                keywords.add(token);
            }
        }
        // 2. 中文技术片段：滑动窗口切 2~4 字，保留非虚词
        StringBuilder chineseRun = new StringBuilder();
        for (int i = 0; i <= question.length(); i++) {
            char ch = i < question.length() ? question.charAt(i) : ' ';
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                chineseRun.append(ch);
            } else {
                collectChineseFragments(chineseRun.toString(), keywords);
                chineseRun.setLength(0);
            }
            if (keywords.size() >= 14) {
                break;
            }
        }
        return keywords;
    }

    /** 把一段连续中文切成 2~4 字片段，剔除虚词与纯停用词 */
    private void collectChineseFragments(String run, List<String> keywords) {
        if (run.length() < 2) {
            return;
        }
        for (int len = 4; len >= 2; len--) {
            for (int i = 0; i + len <= run.length(); i++) {
                if (keywords.size() >= 14) {
                    return;
                }
                String frag = run.substring(i, i + len);
                if (STOP_FRAGMENTS.contains(frag) || containsStopWord(frag)) {
                    continue;
                }
                if (!keywords.contains(frag)) {
                    keywords.add(frag);
                }
            }
        }
    }

    /** 片段中包含常见虚词时判为无意义（如「怎么说」「你会吗」） */
    private boolean containsStopWord(String frag) {
        for (String stop : STOP_WORDS) {
            if (frag.contains(stop)) {
                return true;
            }
        }
        return false;
    }

    /** 把用户配置的 questionCount 收敛到合法范围 */
    private int resolveMaxQuestionCount(Integer input) {
        if (input == null || input < ABSOLUTE_MIN_QUESTIONS) {
            return DEFAULT_MAX_QUESTIONS;
        }
        return Math.min(input, HARD_MAX_QUESTIONS);
    }

    private String lastAiQuestion(Long sessionId) {
        List<InterviewMessage> messages = messageService.listBySessionId(sessionId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (Constants.SENDER_AI.equals(messages.get(i).getSender())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    private String recentTurnsText(Long sessionId) {
        List<InterviewTurn> turns = getTurns(sessionId);
        List<InterviewTurn> recent = turns.size() > 3 ? turns.subList(turns.size() - 3, turns.size()) : turns;
        return turnsText(recent);
    }

    private InterviewSessionResponse buildResponse(InterviewSession session, String currentQuestion,
                                                   boolean finished, InterviewEvaluation evaluation,
                                                   String verdict, String action, String hint,
                                                   Integer totalQuestions) {
        InterviewSessionResponse response = new InterviewSessionResponse();
        response.setSessionId(session.getId());
        response.setJobTitle(session.getJobTitle());
        response.setDifficulty(session.getDifficulty());
        response.setCurrentQuestionIndex(session.getQuestionIndex() == null ? 0 : session.getQuestionIndex());
        response.setCurrentQuestion(currentQuestion);
        response.setFinished(finished);
        response.setEvaluation(evaluation);
        response.setVerdict(verdict);
        response.setAction(action);
        response.setHint(hint);
        response.setTotalQuestions(totalQuestions);
        return response;
    }

    private String resumeText(InterviewSession session) {
        return session.getResumeJson() == null || session.getResumeJson().isBlank()
                ? "（无简历信息）"
                : session.getResumeJson();
    }

    private String resumeToJson(Object resume) {
        if (resume == null) {
            return null;
        }
        return toJson(resume);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("数据序列化失败", e);
        }
    }

    private String turnsText(List<InterviewTurn> turns) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < turns.size(); i++) {
            InterviewTurn turn = turns.get(i);
            // 显式编号，便于评价模型把 questionReviews.index 与题目一一对应
            builder.append("第 ").append(i + 1).append(" 题\n");
            builder.append("问题：").append(turn.getQuestion()).append("\n");
            builder.append("回答：").append(turn.getAnswer()).append("\n\n");
        }
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
