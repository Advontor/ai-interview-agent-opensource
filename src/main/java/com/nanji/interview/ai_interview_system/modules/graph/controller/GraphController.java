package com.nanji.interview.ai_interview_system.modules.graph.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphEdge;
import com.nanji.interview.ai_interview_system.modules.graph.entity.GraphNode;
import com.nanji.interview.ai_interview_system.modules.graph.service.GraphEdgeService;
import com.nanji.interview.ai_interview_system.modules.graph.service.GraphNodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识图谱模块统一接口：
 *  - 节点 CRUD（岗位 / 技能 / 知识点）
 *  - 关系 CRUD（节点之间的关联）
 *  - 按类型查询节点、按节点查询关联关系
 */
@RestController
@RequestMapping("/graph")
public class GraphController {

    @Resource
    private GraphNodeService graphNodeService;

    @Resource
    private GraphEdgeService graphEdgeService;

    // ==================== 节点接口 ====================

    // 新增节点
    @PostMapping("/node")
    public Result<Boolean> addNode(@RequestBody GraphNode node) {
        return Result.success(graphNodeService.save(node));
    }

    // 根据id查询节点
    @GetMapping("/node/{id}")
    public Result<GraphNode> getNode(@PathVariable Long id) {
        return Result.success(graphNodeService.getById(id));
    }

    // 根据节点类型查询节点列表（JOB/SKILL/KNOWLEDGE）
    @GetMapping("/node/type/{nodeType}")
    public Result<List<GraphNode>> getNodeListByType(@PathVariable String nodeType) {
        return Result.success(graphNodeService.listByType(nodeType));
    }

    // 根据名称模糊搜索节点
    @GetMapping("/node/search")
    public Result<List<GraphNode>> searchNode(@RequestParam("keyword") String keyword) {
        return Result.success(graphNodeService.searchByName(keyword));
    }

    // 更新节点
    @PutMapping("/node")
    public Result<Boolean> updateNode(@RequestBody GraphNode node) {
        return Result.success(graphNodeService.updateById(node));
    }

    // 删除节点
    @DeleteMapping("/node/{id}")
    public Result<Boolean> deleteNode(@PathVariable Long id) {
        return Result.success(graphNodeService.removeById(id));
    }

    // ==================== 关系接口 ====================

    // 新增关系
    @PostMapping("/edge")
    public Result<Boolean> addEdge(@RequestBody GraphEdge edge) {
        return Result.success(graphEdgeService.save(edge));
    }

    // 根据id查询关系
    @GetMapping("/edge/{id}")
    public Result<GraphEdge> getEdge(@PathVariable Long id) {
        return Result.success(graphEdgeService.getById(id));
    }

    // 查询某个节点的所有出边（从该节点出发的关系）
    @GetMapping("/edge/source/{nodeId}")
    public Result<List<GraphEdge>> getEdgeBySource(@PathVariable Long nodeId) {
        return Result.success(graphEdgeService.listBySourceNodeId(nodeId));
    }

    // 查询某个节点的所有入边（指向该节点的关系）
    @GetMapping("/edge/target/{nodeId}")
    public Result<List<GraphEdge>> getEdgeByTarget(@PathVariable Long nodeId) {
        return Result.success(graphEdgeService.listByTargetNodeId(nodeId));
    }

    // 更新关系
    @PutMapping("/edge")
    public Result<Boolean> updateEdge(@RequestBody GraphEdge edge) {
        return Result.success(graphEdgeService.updateById(edge));
    }

    // 删除关系
    @DeleteMapping("/edge/{id}")
    public Result<Boolean> deleteEdge(@PathVariable Long id) {
        return Result.success(graphEdgeService.removeById(id));
    }
}
