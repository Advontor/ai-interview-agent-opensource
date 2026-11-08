package com.nanji.interview.ai_interview_system.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 配置（application.yml 中 deepseek.* 前缀，密钥通过环境变量注入）。
 */
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String apiKey;
    private String baseUrl;
    private String model;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
