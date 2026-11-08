package com.nanji.interview.ai_interview_system.modules.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存用户在简历确认页修正后的结构化结果。 */
@Data
public class ResumeProfileUpdateRequest {
    @Valid
    @NotNull(message = "简历内容不能为空")
    private ResumeDTO profile;

    private ResumeScoreResult score;
    private String resumeName;
}
