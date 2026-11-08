package com.nanji.interview.ai_interview_system.modules.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_message")
public class InterviewMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 所属会话id，关联interview_session
    private Long sessionId;
    // 消息发送方：user / ai
    private String sender;
    private String content;
    /**
     * 参考答案（仅 AI 题目的消息在「基于知识库面试」模式下从知识库命中后填入）。
     * 数据库列 reference_answer，由 sql/add_reference_answer.sql 单独迁移。
     */
    private String referenceAnswer;
    private LocalDateTime createTime;
}