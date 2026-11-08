package com.nanji.interview.ai_interview_system.modules.graph.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphEdge;
import com.nanji.interview.ai_interview_system.modules.graph.mapper.GraphEdgeMapper;
import com.nanji.interview.ai_interview_system.modules.graph.service.GraphEdgeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GraphEdgeServiceImpl extends ServiceImpl<GraphEdgeMapper, GraphEdge> implements GraphEdgeService {

    @Override
    public List<GraphEdge> listBySourceNodeId(Long sourceNodeId) {
        return lambdaQuery()
                .eq(GraphEdge::getSourceNodeId, sourceNodeId)
                .list();
    }

    @Override
    public List<GraphEdge> listByTargetNodeId(Long targetNodeId) {
        return lambdaQuery()
                .eq(GraphEdge::getTargetNodeId, targetNodeId)
                .list();
    }
}
