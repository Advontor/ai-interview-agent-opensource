package com.nanji.interview.ai_interview_system.modules.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RAG知识库分块实体
 * 对应表：knowledge_chunk
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String docTitle;

    /** 来源链接或说明 */
    private String docSource;

    /** 岗位标签 */
    private String position;

    /** 分块序号，从0开始 */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** 逗号分隔的向量字符串 */
    private String embedding;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
