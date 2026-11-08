package com.nanji.interview.ai_interview_system.modules.graph.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱关系（节点之间的关联）
 */
@Data
@TableName("graph_edge")
public class GraphEdge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源节点id，关联graph_node */
    private Long sourceNodeId;

    /** 目标节点id，关联graph_node */
    private Long targetNodeId;

    /** 关系类型：REQUIRES需要 / CONTAINS包含 / PREREQUISITE前置 */
    private String relationType;

    /** 关系权重（可选） */
    private Integer weight;

    private LocalDateTime createdAt;
}
