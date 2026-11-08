package com.nanji.interview.ai_interview_system.modules.emotion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 情绪特征（语音面试进阶功能：记录每轮回答的情绪分析结果）
 */
@Data
@TableName("emotion_feature")
public class EmotionFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试会话id，关联interview_session */
    private Long sessionId;

    /** 第几轮回答 */
    private Integer turnIndex;

    /** 情绪类型：紧张 / 自信 / 犹豫 / 平静等 */
    private String emotionType;

    /** 情绪识别置信度 0-100 */
    private BigDecimal confidence;

    /** 对应音频文件路径（可选） */
    private String audioPath;

    private LocalDateTime createdAt;
}
