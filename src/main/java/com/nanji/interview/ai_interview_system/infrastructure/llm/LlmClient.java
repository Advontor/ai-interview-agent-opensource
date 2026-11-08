package com.nanji.interview.ai_interview_system.infrastructure.llm;

import java.util.function.Consumer;

/**
 * 大模型客户端抽象（迁移自 feature/interview-agent）。
 */
public interface LlmClient {

    /**
     * 单轮对话：给定系统提示词与用户提示词，返回模型回复文本。
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式对话：模型每生成一小段文本就回调一次 onToken，全部结束后返回。
     * <p>
     * 默认实现退化为「一次性返回」（不支持流式的实现无需改动，仍可编译运行）。
     * DeepSeek 实现覆盖了该方法，走真实的 SSE 增量输出。
     *
     * @param onToken 增量文本回调（回调可能在 IO 线程触发，实现需自行保证线程安全）
     */
    default void chatStream(String systemPrompt, String userPrompt, Consumer<String> onToken) {
        String full = chat(systemPrompt, userPrompt);
        if (full != null && !full.isEmpty()) {
            onToken.accept(full);
        }
    }
}
