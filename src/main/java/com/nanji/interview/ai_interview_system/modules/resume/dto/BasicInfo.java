package com.nanji.interview.ai_interview_system.modules.resume.dto;

import lombok.Data;

/**
 * 简历基本信息
 * 说明：字符串字段解析不到时为 null（对应需求文档"字符串字段解析不到返回空值"）。
 */
@Data
public class BasicInfo {

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别（男/女），解析不到留空 */
    private String gender;

    /** 年龄，解析不到留空 */
    private String age;

    /** 所在城市，解析不到留空 */
    private String city;
}
