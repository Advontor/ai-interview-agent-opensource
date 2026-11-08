package com.nanji.interview.ai_interview_system.modules.interview.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试历史列表项。
 * <p>
 * 只暴露列表页真正需要的字段：把候选人基础信息（姓名/年龄/城市/学历）从简历 JSON 中解析出来，
 * 便于列表上一眼辨认；同时避免把整份简历 JSON 与 Agent 状态 JSON 一起下发给前端。
 */
@Data
public class InterviewSessionListItem {

    /** 会话 id */
    private Long id;

    /** 面试岗位名称 */
    private String jobTitle;

    /** 面试难度：EASY / MEDIUM / HARD */
    private String difficulty;

    /** 面试状态：ONGOING / FINISHED */
    private String status;

    /** 当前题目数 */
    private Integer questionCount;

    private LocalDateTime createTime;

    private LocalDateTime endTime;

    /** 候选人姓名（简历未填写时为 null） */
    private String candidateName;

    /** 候选人年龄（简历未填写时为 null） */
    private String candidateAge;

    /** 候选人所在城市（简历未填写时为 null） */
    private String candidateCity;

    /** 最高学历，取第一段填写了学历的教育经历 */
    private String candidateEducation;

    /** 绑定的知识库题库标题；为空表示标准面试 */
    private String knowledgeBase;

    /** 面试报告得分；尚未生成报告时为 null */
    private Integer score;
}
