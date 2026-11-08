package com.nanji.interview.ai_interview_system.modules.report.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 报告列表使用的轻量 DTO，避免列表接口传输完整问答和图表数据。 */
@Data
public class ReportListItem {
    private Long reportId;
    private Long sessionId;
    private Integer score;
    private Integer professionalAbility;
    private Integer knowledgeCoverage;
    private Integer jobMatch;
    private Integer communication;
    private Integer logicalThinking;
    private String summary;
    private LocalDateTime createTime;
    private boolean hasTranscript;
    private boolean hasQuestionReviews;
}
