package com.nanji.interview.ai_interview_system.modules.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG知识库配置类
 * 可在application.yml中配置 rag.* 前缀的参数
 *
 * 示例配置：
 * rag:
 *   chunk-size: 500        # 每块最大字符数
 *   chunk-overlap: 50      # 相邻块重叠字符数
 *   top-k: 5               # 检索时返回最相关的前K条
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    /** 每块最大字符数，默认500 */
    private int chunkSize = 500;

    /** 相邻块重叠字符数，默认50 */
    private int chunkOverlap = 50;

    /** 检索时返回最相关的前K条，默认5 */
    private int topK = 5;
}
