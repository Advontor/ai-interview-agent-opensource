package com.nanji.interview.ai_interview_system.modules.interview.media;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 实时语音/视频媒体通道配置。*/
@Configuration
@EnableWebSocket
public class MediaWebSocketConfig implements WebSocketConfigurer {
    private final MediaWebSocketHandler handler;

    public MediaWebSocketConfig(MediaWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/interview/media/ws")
                .setAllowedOriginPatterns("*");
    }
}
