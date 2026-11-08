package com.nanji.interview.ai_interview_system.modules.graph.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphNode;

import java.util.List;

public interface GraphNodeService extends IService<GraphNode> {

    /** 根据节点类型查询节点列表（JOB/SKILL/KNOWLEDGE） */
    List<GraphNode> listByType(String nodeType);

    /** 根据名称模糊搜索节点 */
    List<GraphNode> searchByName(String keyword);
}
