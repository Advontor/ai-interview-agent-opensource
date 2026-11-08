package com.nanji.interview.ai_interview_system.modules.interview.dto;

/**
 * 面试会话响应（创建面试 / 提交回答后返回给前端）。
 * <p>
 * 与旧版内存 InterviewSession DTO 对应，但 sessionId 为数据库主键（Long），
 * 会话与消息均持久化在 interview_session / interview_message 表。
 */
public class InterviewSessionResponse {

    /** 会话 id（interview_session 主键） */
    private Long sessionId;

    /** 面试岗位名称 */
    private String jobTitle;

    /** 面试难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 当前题目序号（从 1 开始） */
    private int currentQuestionIndex;

    /** 当前题目（面试已结束时为 null） */
    private String currentQuestion;

    /** 面试是否已结束 */
    private boolean finished;

    /** 面试评价（仅面试结束时非 null） */
    private InterviewEvaluation evaluation;

    /** 本轮回答判定：VALID / PARTIAL / INVALID / SKIP（恢复会话时为 null） */
    private String verdict;

    /** 本轮动作：RETRY / FOLLOW_UP / NEXT_TOPIC / FINISH（恢复会话时为 null） */
    private String action;

    /** 给候选人的提示（仅 RETRY 时非空） */
    private String hint;

    /**
     * 用户配置的题目总数（来自 InterviewInitRequest.questionCount）。
     * 前端进度条用它来显示「n / total」并判断什么时候到尽头；
     * 后端在 submitAnswer 里用它做硬上限（达到即强制 FINISH）。
     * 为 null 时后端兜底为 10。
     */
    private Integer totalQuestions;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public InterviewEvaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(InterviewEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
}
