package com.nanji.interview.ai_interview_system.modules.graph.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphEdge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GraphEdgeMapper extends BaseMapper<GraphEdge> {
}
