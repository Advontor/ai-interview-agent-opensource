package com.nanji.interview.ai_interview_system.modules.interview.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器（流式输出用）
 * <p>
 * 管理每个面试会话的 SseEmitter 连接，支持向前端推送AI回答的逐字内容。
 * 实际的AI流式调用逻辑由组长在 Agent 层补充，本类仅提供连接管理基础。
 */
@Component
public class SseEmitterManager {

    /** key: sessionId, value: SseEmitter */
    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建一个新的SSE连接
     * @param sessionId 面试会话id
     * @param timeout 超时时间（毫秒），0表示不超时
     */
    public SseEmitter createEmitter(Long sessionId, long timeout) {
        // 如果已有连接，先关闭
        removeEmitter(sessionId);

        SseEmitter emitter = new SseEmitter(timeout);
        emitter.onCompletion(() -> emitterMap.remove(sessionId));
        emitter.onTimeout(() -> emitterMap.remove(sessionId));
        emitter.onError(e -> emitterMap.remove(sessionId));

        emitterMap.put(sessionId, emitter);
        return emitter;
    }

    /**
     * 向指定会话推送消息
     */
    public void send(Long sessionId, Object data) {
        SseEmitter emitter = emitterMap.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (Exception e) {
                emitterMap.remove(sessionId);
            }
        }
    }

    /**
     * 完成指定会话的SSE连接
     */
    public void complete(Long sessionId) {
        SseEmitter emitter = emitterMap.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    /**
     * 移除并关闭指定会话的连接
     */
    public void removeEmitter(Long sessionId) {
        SseEmitter emitter = emitterMap.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    /**
     * 检查会话是否有活跃的SSE连接
     */
    public boolean hasActiveEmitter(Long sessionId) {
        return emitterMap.containsKey(sessionId);
    }
}
