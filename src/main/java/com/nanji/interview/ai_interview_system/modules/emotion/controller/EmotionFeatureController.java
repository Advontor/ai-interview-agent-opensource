package com.nanji.interview.ai_interview_system.modules.emotion.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.emotion.entity.EmotionFeature;
import com.nanji.interview.ai_interview_system.modules.emotion.service.EmotionFeatureService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 情绪特征模块统一接口（语音面试进阶功能）：
 *  - 保存每轮回答的情绪分析结果
 *  - 根据会话id查询情绪记录
 *  - 基础 CRUD
 */
@RestController
@RequestMapping("/emotion")
public class EmotionFeatureController {

    @Resource
    private EmotionFeatureService emotionFeatureService;

    // 保存一条情绪特征记录
    @PostMapping
    public Result<Boolean> saveEmotion(@RequestBody EmotionFeature emotionFeature) {
        return Result.success(emotionFeatureService.save(emotionFeature));
    }

    // 根据id查询情绪特征
    @GetMapping("/{id}")
    public Result<EmotionFeature> getEmotion(@PathVariable Long id) {
        return Result.success(emotionFeatureService.getById(id));
    }

    // 根据会话id查询该场面试所有情绪记录
    @GetMapping("/session/{sessionId}")
    public Result<List<EmotionFeature>> getEmotionsBySessionId(@PathVariable Long sessionId) {
        return Result.success(emotionFeatureService.listBySessionId(sessionId));
    }

    // 更新情绪特征
    @PutMapping
    public Result<Boolean> updateEmotion(@RequestBody EmotionFeature emotionFeature) {
        return Result.success(emotionFeatureService.updateById(emotionFeature));
    }

    // 删除情绪特征
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteEmotion(@PathVariable Long id) {
        return Result.success(emotionFeatureService.removeById(id));
    }
}
