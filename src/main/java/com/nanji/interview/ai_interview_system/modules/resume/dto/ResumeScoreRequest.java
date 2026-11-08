package com.nanji.interview.ai_interview_system.modules.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 请求服务端对一份结构化简历进行评分。 */
@Data
public class ResumeScoreRequest {

    @Valid
    @NotNull(message = "简历内容不能为空")
    private ResumeDTO resume;

    /** 目标岗位，可覆盖解析出来的 targetRole。 */
    private String targetRole;
}
