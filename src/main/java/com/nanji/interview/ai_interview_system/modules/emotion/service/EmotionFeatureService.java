package com.nanji.interview.ai_interview_system.modules.emotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.emotion.entity.EmotionFeature;

import java.util.List;

public interface EmotionFeatureService extends IService<EmotionFeature> {

    /** 根据会话id查询该场面试所有情绪记录 */
    List<EmotionFeature> listBySessionId(Long sessionId);
}
