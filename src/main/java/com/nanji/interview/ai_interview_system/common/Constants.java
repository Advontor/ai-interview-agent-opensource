package com.nanji.interview.ai_interview_system.common;

/**
 * 全局常量。
 */
public final class Constants {

    private Constants() {
    }

    /** 统一返回码：成功 */
    public static final int SUCCESS_CODE = 200;

    /** 统一返回消息：成功 */
    public static final String SUCCESS_MESSAGE = "success";

    /** 面试会话状态：进行中 */
    public static final String SESSION_STATUS_IN_PROGRESS = "ONGOING";

    /** 面试会话状态：已结束 */
    public static final String SESSION_STATUS_FINISHED = "FINISHED";

    /** 消息发送方：AI 面试官 */
    public static final String SENDER_AI = "ai";

    /** 消息发送方：候选人 */
    public static final String SENDER_USER = "user";
}
