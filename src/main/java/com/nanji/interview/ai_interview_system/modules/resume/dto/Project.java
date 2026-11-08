package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目经历
 */
@Data
public class Project {

    /** 项目名称 */
    private String projectName;

    /** 担任角色（如后端开发、组长），解析不到留空 */
    private String role;

    /** 项目开始时间，如 2025.03 */
    private String startTime;

    /** 项目结束时间，如 2025.06 */
    private String endTime;

    /** 项目描述（做了什么、用了什么技术、取得什么成果） */
    private String description;

    /** 项目使用的技术栈，解析不到返回空数组 */
    private List<String> techStack = new ArrayList<>();
}
