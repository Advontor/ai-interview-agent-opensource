package com.nanji.interview.ai_interview_system.modules.voice.controller;

import com.nanji.interview.ai_interview_system.modules.voice.service.VoiceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音面试 WebSocket 端点
 * <p>
 * 前端连接地址：ws://localhost:8080/api/voice/ws/{sessionId}
 * 前端每200ms发一片WebM/Opus音频数据（ArrayBuffer）
 * 后端接收后转码成PCM，调用讯飞识别，把文字结果返回给前端
 */
@Component
@ServerEndpoint("/voice/ws/{sessionId}")
public class VoiceWebSocket {

    /**
     * 用静态变量持有Spring管理的Bean（因为@ServerEndpoint不是Spring管理的）
     */
    private static VoiceService voiceService;

    @Autowired
    public void setVoiceService(VoiceService voiceService) {
        VoiceWebSocket.voiceService = voiceService;
    }

    /**
     * 存储所有连接的会话：key=sessionId, value=WebSocket Session
     */
    private static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @OnOpen
    public void onOpen(@PathParam("sessionId") String sessionId, Session session) {
        SESSION_MAP.put(sessionId, session);
        System.out.println("[语音WebSocket] 连接建立，sessionId=" + sessionId);
        sendMessage(sessionId, "连接成功，可以开始发送音频数据");
    }

    /**
     * 接收前端发来的音频数据（二进制）
     */
    @OnMessage
    public void onMessage(@PathParam("sessionId") String sessionId, byte[] audioData, Session session) {
        if (voiceService != null) {
            try {
                voiceService.processAudio(sessionId, audioData);
            } catch (Exception e) {
                sendMessage(sessionId, "处理音频出错：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId, Session session) {
        SESSION_MAP.remove(sessionId);
        if (voiceService != null) {
            voiceService.finishSession(sessionId);
        }
        System.out.println("[语音WebSocket] 连接关闭，sessionId=" + sessionId);
    }

    /**
     * 连接出错
     */
    @OnError
    public void onError(@PathParam("sessionId") String sessionId, Throwable error) {
        System.err.println("[语音WebSocket] 连接出错，sessionId=" + sessionId + "，错误：" + error.getMessage());
    }

    /**
     * 向指定会话发送消息
     */
    public static void sendMessage(String sessionId, String message) {
        Session session = SESSION_MAP.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向指定会话发送识别结果（JSON格式）
     */
    public static void sendRecognitionResult(String sessionId, String text, boolean isFinal) {
        String json = String.format("{\"type\":\"%s\",\"text\":\"%s\"}", isFinal ? "final" : "partial", text);
        sendMessage(sessionId, json);
    }
}
