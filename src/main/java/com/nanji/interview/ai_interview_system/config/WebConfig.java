package com.nanji.interview.ai_interview_system.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（注册拦截器等）
 * <p>
 * 当前 AuthInterceptor 默认放行所有请求，不影响第一周已有接口。
 * 后续需要启用登录鉴权时，在 addInterceptors 中配置需要拦截的路径。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录鉴权拦截器
        // 当前不拦截任何路径（excludePathPatterns 排除所有）
        // 后续启用鉴权时，修改为：
        //   registry.addInterceptor(authInterceptor)
        //       .addPathPatterns("/interview/**", "/report/**", "/user/**")
        //       .excludePathPatterns("/user/login", "/user/register", "/resume/upload");
        registry.addInterceptor(authInterceptor)
                .excludePathPatterns("/**");
    }
}
