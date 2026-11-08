package com.nanji.interview.ai_interview_system.config;

/**
 * 用户上下文（ThreadLocal，存储当前登录用户id）
 * <p>
 * 后续做接口鉴权时，拦截器从请求头解析用户id并存入这里，
 * Controller/Service 可通过 UserContext.getUserId() 获取当前登录用户。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
