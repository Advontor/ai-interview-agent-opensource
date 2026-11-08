package com.nanji.interview.ai_interview_system.modules.voice.service;

/**
 * 语音处理服务接口
 */
public interface VoiceService {

    /**
     * 处理前端发来的音频数据
     *
     * @param sessionId 面试会话id
     * @param audioData WebM/Opus格式的音频数据（一片）
     */
    void processAudio(String sessionId, byte[] audioData);

    /**
     * 结束语音会话（前端断开连接时调用）
     * 做清理工作：删除临时文件、结束讯飞识别等
     *
     * @param sessionId 面试会话id
     */
    void finishSession(String sessionId);
}
