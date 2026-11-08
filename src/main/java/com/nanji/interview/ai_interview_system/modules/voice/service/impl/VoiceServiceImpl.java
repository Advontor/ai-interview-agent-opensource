package com.nanji.interview.ai_interview_system.modules.voice.service.impl;

import com.nanji.interview.ai_interview_system.modules.voice.controller.VoiceWebSocket;
import com.nanji.interview.ai_interview_system.modules.voice.service.VoiceService;
import com.nanji.interview.ai_interview_system.modules.voice.service.XfyunService;
import com.nanji.interview.ai_interview_system.modules.voice.util.AudioConverter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音处理服务实现
 * <p>
 * 流程：
 * 1. 接收前端发来的WebM/Opus音频数据，写入临时文件
 * 2. 前端断开连接后，用FFmpeg转码成16kHz PCM
 * 3. 调用讯飞语音识别
 * 4. 把识别结果通过WebSocket返回给前端
 */
@Service
public class VoiceServiceImpl implements VoiceService {

    @Resource
    private AudioConverter audioConverter;

    @Resource
    private XfyunService xfyunService;

    /**
     * 临时文件目录
     */
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/ai_interview_voice/";

    /**
     * 存储每个会话的临时文件输出流：key=sessionId, value=FileOutputStream
     */
    private final Map<String, FileOutputStream> outputStreamMap = new ConcurrentHashMap<>();

    /**
     * 存储每个会话的临时文件路径：key=sessionId, value=文件路径
     */
    private final Map<String, String> tempFilePathMap = new ConcurrentHashMap<>();

    @Override
    public void processAudio(String sessionId, byte[] audioData) {
        try {
            // 第一次收到数据时，创建临时文件
            if (!outputStreamMap.containsKey(sessionId)) {
                File dir = new File(TEMP_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String tempFilePath = TEMP_DIR + sessionId + "_" + System.currentTimeMillis() + ".webm";
                FileOutputStream fos = new FileOutputStream(tempFilePath, true);
                outputStreamMap.put(sessionId, fos);
                tempFilePathMap.put(sessionId, tempFilePath);
                System.out.println("[语音处理] 开始接收音频，sessionId=" + sessionId + "，临时文件：" + tempFilePath);
            }

            // 写入音频数据
            FileOutputStream fos = outputStreamMap.get(sessionId);
            fos.write(audioData);
            fos.flush();

        } catch (IOException e) {
            VoiceWebSocket.sendMessage(sessionId, "保存音频数据出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void finishSession(String sessionId) {
        try {
            // 关闭文件输出流
            FileOutputStream fos = outputStreamMap.remove(sessionId);
            if (fos != null) {
                fos.close();
            }

            String webmFilePath = tempFilePathMap.remove(sessionId);
            if (webmFilePath == null || !new File(webmFilePath).exists()) {
                System.out.println("[语音处理] 没有收到音频数据，sessionId=" + sessionId);
                return;
            }

            System.out.println("[语音处理] 结束接收音频，开始转码，sessionId=" + sessionId);

            // 用FFmpeg转码成PCM
            String pcmFilePath = webmFilePath.replace(".webm", ".pcm");
            boolean convertSuccess = audioConverter.convertToPcm(webmFilePath, pcmFilePath);

            if (!convertSuccess) {
                VoiceWebSocket.sendMessage(sessionId, "音频转码失败，请重试");
                return;
            }

            System.out.println("[语音处理] 转码成功，开始讯飞识别，sessionId=" + sessionId);

            // 调用讯飞语音识别（你来实现）
            String result = xfyunService.recognize(pcmFilePath);

            if (result != null && !result.isBlank()) {
                // 把识别结果返回给前端
                VoiceWebSocket.sendRecognitionResult(sessionId, result, true);
                System.out.println("[语音处理] 识别完成，sessionId=" + sessionId + "，结果：" + result);
            } else {
                VoiceWebSocket.sendMessage(sessionId, "识别结果为空，请重试");
            }

            // 清理临时文件
            new File(webmFilePath).delete();
            new File(pcmFilePath).delete();

        } catch (Exception e) {
            VoiceWebSocket.sendMessage(sessionId, "语音处理出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
