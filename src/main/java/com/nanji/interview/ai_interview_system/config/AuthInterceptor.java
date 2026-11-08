package com.nanji.interview.ai_interview_system.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器（基础框架）
 * <p>
 * 当前默认放行所有请求（第一周无登录体系，不拦截）。
 * 后续需要启用鉴权时：
 * 1. 在 WebConfig 中配置需要拦截的路径（如 /interview/**、/report/**）
 * 2. 在 preHandle 中从请求头解析用户id（如从 Authorization 头或 Cookie）
 * 3. 校验用户是否登录，未登录返回401
 * <p>
 * 注意：当前实现仅为框架，不影响第一周已有接口。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // TODO: 后续启用鉴权时，从请求头解析用户id并校验
        // String token = request.getHeader("Authorization");
        // if (token == null || token.isBlank()) {
        //     response.setStatus(401);
        //     return false;
        // }
        // Long userId = parseUserIdFromToken(token);
        // UserContext.setUserId(userId);

        // 当前默认放行所有请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
