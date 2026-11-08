package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历结构化数据对象（简历解析模块的核心产出）。
 *
 * 空值规则（需求文档第 4.3 节）：
 *  - 数组类型字段没有内容：返回空数组（绝不为 null）
 *  - 对象类型字段完全解析不到：返回空对象（不为 null）
 *  - 字符串字段解析不到：返回 null
 */
@Data
public class ResumeDTO {

    /** 基本信息 */
    private BasicInfo basicInfo = new BasicInfo();

    /** 教育经历列表 */
    private List<Education> educationList = new ArrayList<>();

    /** 项目经历列表 */
    private List<Project> projectList = new ArrayList<>();

    /** 专业技能列表，每项为一个独立技能字符串 */
    private List<String> skills = new ArrayList<>();

    /** 实习经历列表 */
    private List<Internship> internshipList = new ArrayList<>();

    /** 证书列表 */
    private List<String> certificates = new ArrayList<>();

    /** 解析过程中的警告信息，没有警告时为空数组 */
    private List<String> warnings = new ArrayList<>();

    /** 目标岗位 / 求职意向，解析不到为 null */
    private String targetRole;
}
