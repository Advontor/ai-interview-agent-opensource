package com.nanji.interview.ai_interview_system.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型配置。
 *
 * application.yml 中使用 llm.* 前缀。
 * API Key 通过环境变量 DASHSCOPE_API_KEY 注入。
 *
 * 支持一个主模型和多个备用模型。
 */
@Component
@ConfigurationProperties(prefix = "llm")
public class DashScopeProperties {

    private String apiKey;
    private String baseUrl;

    /**
     * 主模型。
     */
    private String model;

    /**
     * 备用模型列表。
     * 主模型因额度、限流或服务异常不可用时依次尝试。
     */
    private List<String> fallbackModels = new ArrayList<>();

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

    public List<String> getFallbackModels() {
        return fallbackModels;
    }

    public void setFallbackModels(List<String> fallbackModels) {
        this.fallbackModels = fallbackModels;
    }

    /**
     * 按调用优先级返回全部模型：
     * 主模型 -> 备用模型1 -> 备用模型2...
     */
    public List<String> getAllModels() {
        List<String> models = new ArrayList<>();

        if (model != null && !model.isBlank()) {
            models.add(model);
        }

        if (fallbackModels != null) {
            for (String fallbackModel : fallbackModels) {
                if (fallbackModel != null
                        && !fallbackModel.isBlank()
                        && !models.contains(fallbackModel)) {
                    models.add(fallbackModel);
                }
            }
        }

        return models;
    }
}
