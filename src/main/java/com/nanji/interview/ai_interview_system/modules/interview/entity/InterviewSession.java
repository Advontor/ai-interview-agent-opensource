package com.nanji.interview.ai_interview_system.modules.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试会话（第一周新增字段：difficulty、questionIndex、resumeJson，
 * 建表/升级语句见 sql/week1_integration.sql）。
 */
@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 用户id，关联user表（第一周无登录，可为空）
    private Long userId;
    // 面试岗位名称
    private String jobTitle;
    // 面试难度：EASY / MEDIUM / HARD
    private String difficulty;
    // 面试状态：ONGOING / FINISHED
    private String status;
    // 当前题目序号（从1开始）
    private Integer questionIndex;
    // 简历结构化数据（ResumeDTO JSON），供生成题目与评价使用
    private String resumeJson;
    // Agent 面试状态（AgentState JSON）：有效题数/知识点覆盖/追问次数等，刷新后可从库恢复
    private String agentStateJson;
    private LocalDateTime createTime;
    private LocalDateTime endTime;
}
