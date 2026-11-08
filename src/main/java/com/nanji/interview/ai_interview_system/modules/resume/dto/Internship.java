package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

/**
 * 实习经历
 */
@Data
public class Internship {

    /** 公司名称 */
    private String company;

    /** 职位 */
    private String position;

    /** 开始时间，如 2025.07 */
    private String startTime;

    /** 结束时间，如 2025.09 */
    private String endTime;

    /** 工作内容描述 */
    private String description;
}
