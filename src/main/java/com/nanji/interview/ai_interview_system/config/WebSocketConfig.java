package com.nanji.interview.ai_interview_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置（语音实时面试进阶功能用）
 * <p>
 * 启用后可通过 @ServerEndpoint 注解创建 WebSocket 端点，
 * 用于前端与后端实时传输音频流。
 * 注意：实际语音处理逻辑由组长/前端联调时补充，本配置仅启用 WebSocket 支持。
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
