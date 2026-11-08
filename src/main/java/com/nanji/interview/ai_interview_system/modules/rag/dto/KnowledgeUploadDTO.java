package com.nanji.interview.ai_interview_system.modules.rag.dto;

import lombok.Data;

/**
 * 知识库文档上传请求DTO
 * 用于上传整篇文档，后端自动分块后存入knowledge_chunk
 */
@Data
public class KnowledgeUploadDTO {

    /** 文档标题 */
    private String docTitle;

    /** 来源链接或说明 */
    private String docSource;

    /** 岗位标签 */
    private String position;

    /** 文档全文内容（后端自动分块） */
    private String content;
}
