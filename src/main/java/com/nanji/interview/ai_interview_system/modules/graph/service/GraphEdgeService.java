package com.nanji.interview.ai_interview_system.modules.graph.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphEdge;

import java.util.List;

public interface GraphEdgeService extends IService<GraphEdge> {

    /** 查询某个节点的所有出边（从该节点出发的关系） */
    List<GraphEdge> listBySourceNodeId(Long sourceNodeId);

    /** 查询某个节点的所有入边（指向该节点的关系） */
    List<GraphEdge> listByTargetNodeId(Long targetNodeId);
}
