package com.nanji.interview.ai_interview_system.modules.interview.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 媒体流通道的第一版骨架。
 *
 * 当前负责会话握手、媒体分片计数和状态回执，避免浏览器端在未来接入 ASR/TTS
 * 时重复设计协议。真正的 DashScope 音频适配器可以在此处按 sessionId 注入。
 */
@Component
public class MediaWebSocketHandler extends AbstractWebSocketHandler {
    private final ObjectMapper objectMapper;

    public MediaWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        send(session, "media.ready", Map.of(
                "connectionId", session.getId(),
                "transcription", "provider-pending",
                "tts", "provider-pending"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "media.ack");
        payload.put("received", message.getPayloadLength());
        payload.put("message", "媒体通道已连接；当前版本先回执分片，ASR/TTS 供应商适配器可独立接入");
        send(session, "media.ack", payload);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        send(session, "transcript.partial", Map.of(
                "bytes", message.getPayloadLength(),
                "text", "",
                "provider", "pending"
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 媒体会话关闭不影响文字面试状态，前端可以根据 revision 恢复。
    }

    private void send(WebSocketSession session, String type, Object payload) throws IOException {
        if (!session.isOpen()) return;
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("eventId", System.nanoTime());
        event.put("payload", payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
    }
}
