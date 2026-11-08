package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

/**
 * 教育经历
 * 时间统一为"年.月"格式，如 2021.09；解析不到为 null。
 */
@Data
public class Education {

    /** 学校名称 */
    private String school;

    /** 专业 */
    private String major;

    /** 学历（本科/硕士/博士/大专） */
    private String degree;

    /** 入学时间，如 2021.09 */
    private String startTime;

    /** 毕业时间，如 2025.06 */
    private String endTime;

    /** GPA，解析不到留空 */
    private String gpa;
}
