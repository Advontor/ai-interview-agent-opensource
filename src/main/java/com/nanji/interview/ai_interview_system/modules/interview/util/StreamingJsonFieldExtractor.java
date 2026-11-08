package com.nanji.interview.ai_interview_system.modules.interview.util;

import java.util.function.Consumer;

/**
 * 流式 JSON 字段提取器。
 * <p>
 * 解决的问题：Agent 一次调用要返回结构化 JSON（判定 + 决策 + 下一题 + 记忆更新），
 * 而 JSON 必须整体生成完才能解析——这与「逐字流式输出」天然冲突。
 * <p>
 * 本类不等待 JSON 完整，而是边接收模型增量边扫描，
 * 一旦进入目标字段的字符串值就开始逐字符向外吐出（已解码转义），
 * 从而让「下一道题」在模型还在生成后续字段时就已经推送到前端。
 * <p>
 * 配套要求：提示词中该字段应放在 JSON 的第一位，否则用户仍需等前面字段生成完。
 *
 * 用法：
 * <pre>
 *   var ex = new StreamingJsonFieldExtractor("nextQuestion", token -&gt; push(token));
 *   llmClient.chatStream(sys, user, ex::push);
 *   String full = ex.getBufferText();  // 完整 JSON，供常规解析
 * </pre>
 */
public final class StreamingJsonFieldExtractor {

    private static final int SEEK_KEY = 0;
    private static final int SEEK_COLON = 1;
    private static final int IN_VALUE = 2;
    private static final int DONE = 3;

    private final String quotedKey;
    private final Consumer<String> sink;

    /** 累积的完整原始文本（流结束后可直接交给 Jackson 解析） */
    private final StringBuilder buffer = new StringBuilder();

    /** 已吐出的目标字段值 */
    private final StringBuilder value = new StringBuilder();

    private int scanPos = 0;
    private int state = SEEK_KEY;
    private boolean escape = false;
    private boolean found = false;
    private boolean closed = false;

    /** 四位十六进制的码点转义可能被 chunk 边界切断，需单独缓存 */
    private final StringBuilder unicodeBuf = new StringBuilder();
    private int unicodeRemaining = 0;

    public StreamingJsonFieldExtractor(String fieldName, Consumer<String> sink) {
        this.quotedKey = "\"" + fieldName + "\"";
        this.sink = sink;
    }

    /** 接收模型增量文本 */
    public void push(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        buffer.append(chunk);
        drain();
    }

    private void drain() {
        while (true) {
            if (state == DONE) {
                return;
            }
            if (state == SEEK_KEY) {
                // 保留尾部 quotedKey.length()-1 个字符：key 可能被 chunk 边界切断，等下一帧再判
                int limit = buffer.length() - (quotedKey.length() - 1);
                if (limit <= scanPos) {
                    return;
                }
                int idx = buffer.indexOf(quotedKey, scanPos);
                if (idx < 0 || idx > limit) {
                    scanPos = Math.max(scanPos, limit);
                    return;
                }
                scanPos = idx + quotedKey.length();
                state = SEEK_COLON;
                continue;
            }
            if (state == SEEK_COLON) {
                while (scanPos < buffer.length()) {
                    char c = buffer.charAt(scanPos++);
                    if (c == '"') {
                        state = IN_VALUE;
                        found = true;
                        break;
                    }
                    if (c == '{' || c == '[') {
                        // 异常情况：字段值是对象/数组，本提取器只处理字符串，放弃提取
                        state = DONE;
                        break;
                    }
                }
                if (state == DONE) {
                    return;
                }
                if (state != IN_VALUE) {
                    return; // 还没读到值的起始引号，等下一帧
                }
                continue;
            }
            if (state == IN_VALUE) {
                while (scanPos < buffer.length()) {
                    char c = buffer.charAt(scanPos++);

                    if (unicodeRemaining > 0) {
                        unicodeBuf.append(c);
                        unicodeRemaining--;
                        if (unicodeRemaining == 0) {
                            emitUnicode();
                        }
                        continue;
                    }

                    if (escape) {
                        escape = false;
                        switch (c) {
                            case 'n' -> emit('\n');
                            case 't' -> emit('\t');
                            case 'r' -> emit('\r');
                            case 'b' -> emit('\b');
                            case 'f' -> emit('\f');
                            case '"' -> emit('"');
                            case '\\' -> emit('\\');
                            case '/' -> emit('/');
                            case 'u' -> {
                                unicodeBuf.setLength(0);
                                unicodeRemaining = 4;
                            }
                            default -> emit(c);
                        }
                        continue;
                    }

                    if (c == '\\') {
                        escape = true;
                        continue;
                    }
                    if (c == '"') {
                        closed = true;
                        state = DONE;
                        return;
                    }
                    emit(c);
                }
                return; // 本帧字符处理完，等下一帧
            }
            return;
        }
    }

    private void emitUnicode() {
        try {
            emit((char) Integer.parseInt(unicodeBuf.toString(), 16));
        } catch (Exception ignored) {
            // 非法 unicode 转义，忽略该字符，不中断流
        }
        unicodeBuf.setLength(0);
    }

    private void emit(char c) {
        value.append(c);
        if (sink != null) {
            sink.accept(String.valueOf(c));
        }
    }

    /** 已提取出的目标字段值（流未结束时是已收到的部分） */
    public String getValue() {
        return value.toString();
    }

    /** 目标字段是否已在流中出现（值为空串也算出现） */
    public boolean isFound() {
        return found;
    }

    /** 目标字段的值是否已完整闭合 */
    public boolean isClosed() {
        return closed;
    }

    /** 流结束后取完整原始文本，交给 Jackson 做常规解析 */
    public String getBufferText() {
        return buffer.toString();
    }
}
