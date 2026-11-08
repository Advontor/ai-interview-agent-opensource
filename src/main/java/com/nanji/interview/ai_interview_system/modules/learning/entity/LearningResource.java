package com.nanji.interview.ai_interview_system.modules.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习资源（Agent 根据面试薄弱点推送学习资源用）
 */
@Data
@TableName("learning_resource")
public class LearningResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源标题 */
    private String title;

    /** 资源链接 */
    private String url;

    /** 分类：Java基础 / 算法 / 项目经验 / 操作系统等 */
    private String category;

    /** 关联技能标签（用于匹配面试薄弱点） */
    private String relatedSkill;

    /** 资源描述 */
    private String description;

    private LocalDateTime createdAt;
}
