package com.nanji.interview.ai_interview_system.modules.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.learning.entity.LearningResource;

import java.util.List;

public interface LearningResourceService extends IService<LearningResource> {

    /** 根据分类查询学习资源 */
    List<LearningResource> listByCategory(String category);

    /** 根据关联技能标签查询学习资源（匹配面试薄弱点） */
    List<LearningResource> listByRelatedSkill(String relatedSkill);
}
