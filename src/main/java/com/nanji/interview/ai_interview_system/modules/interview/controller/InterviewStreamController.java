package com.nanji.interview.ai_interview_system.modules.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewSessionResponse;
import com.nanji.interview.ai_interview_system.modules.interview.dto.SubmitAnswerRequest;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewSessionService;
import com.nanji.interview.ai_interview_system.modules.interview.util.SseEmitterManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 面试流式输出接口（SSE，Server-Sent Events）。
 * <p>
 * 与同步接口 {@code POST /interview/answer} 业务完全一致，
 * 区别是模型生成「下一道题」时逐字推送给前端（真流式）。
 * <p>
 * 前端需使用 fetch + ReadableStream 接收——原生 EventSource 不支持 POST。
 * <p>
 * 事件格式（每条均为 JSON）：
 * <ul>
 *   <li>{@code {"type":"token","text":"字"}} 下一题的增量文本，可能多次</li>
 *   <li>{@code {"type":"done","data":{...InterviewSessionResponse...}}} 本轮最终结果</li>
 *   <li>{@code {"type":"error","message":"..."}} 失败</li>
 * </ul>
 */
@RestController
@RequestMapping("/interview/stream")
public class InterviewStreamController {

    private static final long TIMEOUT_MS = 5 * 60 * 1000L;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private InterviewSessionService interviewSessionService;

    @Resource
    private ObjectMapper objectMapper;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        // 演示规模并发很低，固定 8 线程足够；避免每轮面试都新建线程
        executor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "interview-stream");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 建立 SSE 连接（保留：前端若只需单向推送可用 EventSource 连接此接口）
     */
    @GetMapping(value = "/connect/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable Long sessionId) {
        return sseEmitterManager.createEmitter(sessionId, TIMEOUT_MS);
    }

    /**
     * 提交回答并流式返回 AI 的下一道题。
     */
    @PostMapping("/answer/{sessionId}")
    public SseEmitter submitAnswerStream(@PathVariable Long sessionId,
                                         @RequestBody(required = false) SubmitAnswerRequest request,
                                         HttpServletResponse response) {
        // 关闭 Nginx 代理缓冲，否则 SSE 会被攒在缓冲区里一次性吐出，前端看不到逐字效果
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        String answer = request == null ? "" : request.getAnswer();
        if (request != null) {
            request.setSessionId(sessionId);
        }

        SseEmitter emitter = sseEmitterManager.createEmitter(sessionId, TIMEOUT_MS);

        executor.submit(() -> runStream(sessionId, answer));

        return emitter;
    }

    private void runStream(Long sessionId, String answer) {
        AtomicReference<InterviewSessionResponse> finalResult = new AtomicReference<>();
        try {
            interviewSessionService.submitAnswerStream(
                    sessionId,
                    answer,
                    // 模型每生成一小段题目文本就推一次
                    token -> sseEmitterManager.send(sessionId, event("token", token, null)),
                    finalResult::set
            );
            sseEmitterManager.send(sessionId, event("done", null, finalResult.get()));
        } catch (Exception e) {
            // 流式链路失败（网络/解析/超时）：降级走原同步接口，保证面试不中断
            try {
                SubmitAnswerRequest fallback = new SubmitAnswerRequest();
                fallback.setSessionId(sessionId);
                fallback.setAnswer(answer);
                InterviewSessionResponse resp = interviewSessionService.submitAnswer(fallback);
                sseEmitterManager.send(sessionId, event("done", null, resp));
            } catch (Exception ex) {
                sseEmitterManager.send(sessionId, event("error", describe(ex), null));
            }
        } finally {
            sseEmitterManager.complete(sessionId);
        }
    }

    private Map<String, Object> event(String type, String text, Object data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        if (text != null) {
            event.put("text", text);
        }
        if (data != null) {
            event.put("data", data);
        }
        return event;
    }

    private String describe(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    /**
     * 断开 SSE 连接
     */
    @PostMapping("/disconnect/{sessionId}")
    public void disconnect(@PathVariable Long sessionId) {
        sseEmitterManager.removeEmitter(sessionId);
    }
}
