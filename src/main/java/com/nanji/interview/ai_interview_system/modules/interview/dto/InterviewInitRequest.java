package com.nanji.interview.ai_interview_system.modules.interview.dto;

import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建面试请求。
 * <p>
 * resume 直接引用简历解析模块的唯一标准结构 ResumeDTO
 * （不再使用旧的 ResumeParseResult / ResumeInfo 等重复结构）。
 */
public class InterviewInitRequest {

    /** 面试岗位名称 */
    @NotBlank(message = "面试岗位不能为空")
    private String jobTitle;

    /** 岗位描述（可选） */
    private String jobDescription;

    /** 面试难度：EASY / MEDIUM / HARD */
    @NotBlank(message = "面试难度不能为空")
    private String difficulty;

    /** 简历结构化数据（ResumeDTO） */
    private ResumeDTO resume;

    /**
     * 知识库文档标题（可选）。
     * 传入后视为「基于知识库面试」：出题时检索该题库的真题作为参考，
     * 使问题更贴合目标岗位的考察范围。为空时退化为普通面试。
     */
    private String knowledgeBase;

    /**
     * 期望的题目数量（用户在前端配置，5~20 之间）。
     * 不传或非法值时后端兜底使用 10。后端会把它写到 AgentState，
     * 并在 submitAnswer 里用它做硬上限（达到即强制 FINISH，不再依赖 Agent LLM 的不稳定决策）。
     */
    private Integer questionCount;

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public ResumeDTO getResume() {
        return resume;
    }

    public void setResume(ResumeDTO resume) {
        this.resume = resume;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
}
