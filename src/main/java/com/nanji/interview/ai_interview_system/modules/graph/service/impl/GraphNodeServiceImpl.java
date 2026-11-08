package com.nanji.interview.ai_interview_system.modules.graph.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphNode;
import com.nanji.interview.ai_interview_system.modules.graph.mapper.GraphNodeMapper;
import com.nanji.interview.ai_interview_system.modules.graph.service.GraphNodeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class GraphNodeServiceImpl extends ServiceImpl<GraphNodeMapper, GraphNode> implements GraphNodeService {

    @Override
    public List<GraphNode> listByType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(GraphNode::getNodeType, nodeType)
                .list();
    }

    @Override
    public List<GraphNode> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .like(GraphNode::getName, keyword)
                .list();
    }
}
