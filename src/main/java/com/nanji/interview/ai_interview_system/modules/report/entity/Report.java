package com.nanji.interview.ai_interview_system.modules.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试报告（第一周新增字段：summary、professionalAbility、knowledgeCoverage、
 * jobMatch、communication、logicalThinking；改进建议以 JSON 字符串数组形式
 * 存入原有 suggestion 字段。建表/升级语句见 sql/week1_integration.sql）。
 */
@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** 综合得分 0-100 */
    private Integer score;
    /** 亮点（对应评价 strengths） */
    private String advantage;
    /** 薄弱点（对应评价 weaknesses） */
    private String disadvantage;
    /** 改进建议（JSON 字符串数组，对应评价 suggestions） */
    private String suggestion;
    /** 总体评价 */
    private String summary;
    /** 专业能力 0-100 */
    private Integer professionalAbility;
    /** 知识覆盖度 0-100 */
    private Integer knowledgeCoverage;
    /** 岗位匹配度 0-100 */
    private Integer jobMatch;
    /** 表达沟通 0-100 */
    private Integer communication;
    /** 逻辑结构 0-100 */
    private Integer logicalThinking;
    /** 本次面试的应聘岗位（冗余存储，报告页展示用） */
    private String jobTitle;
    /** 本次面试难度：EASY / MEDIUM / HARD */
    private String difficulty;
    /** 逐题评分与点评（JSON 数组，元素含 index/score/comment/question/answer/referenceAnswer） */
    private String questionReviews;
    private LocalDateTime createTime;
}
