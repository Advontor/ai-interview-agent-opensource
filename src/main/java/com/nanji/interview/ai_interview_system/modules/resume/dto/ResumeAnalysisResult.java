package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

/** 简历解析结果和评分结果的组合，供上传页直接预览和自动填充。 */
@Data
public class ResumeAnalysisResult {
    private ResumeDTO profile;
    private ResumeScoreResult score;
}
