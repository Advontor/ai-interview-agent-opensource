package com.nanji.interview.ai_interview_system.modules.voice.service;

/**
 * 讯飞语音识别服务接口
 * <p>
 * 你来实现具体逻辑！需要做的事：
 * 1. 配置讯飞的APPID、APIKey、APISecret（可以写在application.yml里）
 * 2. 调用讯飞语音识别WebSocket API（wss://iat-api.xfyun.cn/v2/iat）
 * 3. 把PCM音频数据一片片传给讯飞
 * 4. 接收讯飞返回的识别结果，拼接成完整文字
 * 5. 返回识别结果
 * <p>
 * 讯飞文档：https://www.xfyun.cn/doc/asr/voicedictation/API.html
 * <p>
 * 音频格式要求（已经由AudioConverter转好）：
 * - 采样率：16000 Hz
 * - 声道：单声道
 * - 位深：16位
 * - 格式：PCM（s16le）
 */
public interface XfyunService {

    /**
     * 语音识别
     *
     * @param pcmFilePath PCM音频文件路径（16kHz 16位单声道）
     * @return 识别出的文字结果
     */
    String recognize(String pcmFilePath);
}
