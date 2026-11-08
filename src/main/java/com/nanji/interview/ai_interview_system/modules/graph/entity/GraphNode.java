package com.nanji.interview.ai_interview_system.modules.graph.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱节点（岗位 / 技能 / 知识点）
 */
@Data
@TableName("graph_node")
public class GraphNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节点类型：JOB岗位 / SKILL技能 / KNOWLEDGE知识点 */
    private String nodeType;

    /** 节点名称 */
    private String name;

    /** 节点描述 */
    private String description;

    private LocalDateTime createdAt;
}
