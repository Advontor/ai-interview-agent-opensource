package com.nanji.interview.ai_interview_system.modules.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.learning.entity.LearningResource;
import com.nanji.interview.ai_interview_system.modules.learning.mapper.LearningResourceMapper;
import com.nanji.interview.ai_interview_system.modules.learning.service.LearningResourceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class LearningResourceServiceImpl extends ServiceImpl<LearningResourceMapper, LearningResource> implements LearningResourceService {

    @Override
    public List<LearningResource> listByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(LearningResource::getCategory, category)
                .list();
    }

    @Override
    public List<LearningResource> listByRelatedSkill(String relatedSkill) {
        if (!StringUtils.hasText(relatedSkill)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(LearningResource::getRelatedSkill, relatedSkill)
                .list();
    }
}
