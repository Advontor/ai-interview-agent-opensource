package com.nanji.interview.ai_interview_system.modules.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.common.util.JsonUtils;
import com.nanji.interview.ai_interview_system.infrastructure.llm.LlmClient;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import com.nanji.interview.ai_interview_system.modules.resume.prompt.ResumeParsePrompt;
import org.springframework.stereotype.Service;

/**
 * 简历解析服务（迁移自 feature/interview-agent 的 ResumeParseService）：
 * 调用 DeepSeek 把简历纯文本解析为 ResumeDTO（唯一标准简历结构，
 * 不再使用旧的自定义 ResumeParseResult）。
 */
@Service
public class ResumeParseService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ResumeParseService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 把简历纯文本解析为 ResumeDTO。
     *
     * @param resumeText 简历文件提取出的纯文字
     * @return 结构化简历数据
     */
    public ResumeDTO parse(String resumeText) {
        String jsonResult = llmClient.chat(
                ResumeParsePrompt.SYSTEM_PROMPT,
                resumeText
        );

        try {
            return objectMapper.readValue(
                    JsonUtils.cleanCodeFence(jsonResult),
                    ResumeDTO.class
            );
        } catch (Exception e) {
            throw new BusinessException("简历解析结果不是合法 JSON", e);
        }
    }
}
