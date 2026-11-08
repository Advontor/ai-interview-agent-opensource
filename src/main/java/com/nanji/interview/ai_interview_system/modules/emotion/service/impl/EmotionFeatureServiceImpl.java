package com.nanji.interview.ai_interview_system.modules.emotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.emotion.entity.EmotionFeature;
import com.nanji.interview.ai_interview_system.modules.emotion.mapper.EmotionFeatureMapper;
import com.nanji.interview.ai_interview_system.modules.emotion.service.EmotionFeatureService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmotionFeatureServiceImpl extends ServiceImpl<EmotionFeatureMapper, EmotionFeature> implements EmotionFeatureService {

    @Override
    public List<EmotionFeature> listBySessionId(Long sessionId) {
        return lambdaQuery()
                .eq(EmotionFeature::getSessionId, sessionId)
                .orderByAsc(EmotionFeature::getTurnIndex)
                .list();
    }
}
