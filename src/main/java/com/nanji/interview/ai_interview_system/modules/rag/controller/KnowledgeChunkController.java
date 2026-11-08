package com.nanji.interview.ai_interview_system.modules.rag.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.rag.entity.KnowledgeChunk;
import com.nanji.interview.ai_interview_system.modules.rag.service.KnowledgeChunkService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG知识库分块 Controller
 * 提供知识库的增删改查、按岗位/文档查询、全文检索等接口
 */
@RestController
@RequestMapping("/rag")
public class KnowledgeChunkController {

    @Resource
    private KnowledgeChunkService knowledgeChunkService;

    /**
     * 新增一条知识库分块
     */
    @PostMapping("/chunk")
    public Result<Boolean> addChunk(@RequestBody KnowledgeChunk chunk) {
        return Result.success(knowledgeChunkService.save(chunk));
    }

    /**
     * 根据id查询分块详情
     */
    @GetMapping("/chunk/{id}")
    public Result<KnowledgeChunk> getChunk(@PathVariable Long id) {
        return Result.success(knowledgeChunkService.getById(id));
    }

    /**
     * 按岗位标签查询所有分块
     */
    @GetMapping("/chunk/position/{position}")
    public Result<List<KnowledgeChunk>> listByPosition(@PathVariable String position) {
        return Result.success(knowledgeChunkService.listByPosition(position));
    }

    /**
     * 按文档标题查询所有分块
     */
    @GetMapping("/chunk/doc/{docTitle}")
    public Result<List<KnowledgeChunk>> listByDocTitle(@PathVariable String docTitle) {
        return Result.success(knowledgeChunkService.listByDocTitle(docTitle));
    }

    /**
     * 全文检索（按content模糊匹配）
     */
    @GetMapping("/search")
    public Result<List<KnowledgeChunk>> search(@RequestParam String keyword) {
        return Result.success(knowledgeChunkService.searchByKeyword(keyword));
    }

    /**
     * 查询所有不重复的文档标题列表
     */
    @GetMapping("/doc/list")
    public Result<List<String>> listDocTitles() {
        return Result.success(knowledgeChunkService.listDocTitles());
    }

    /**
     * 更新分块
     */
    @PutMapping("/chunk")
    public Result<Boolean> updateChunk(@RequestBody KnowledgeChunk chunk) {
        return Result.success(knowledgeChunkService.updateById(chunk));
    }

    /**
     * 根据id删除单条分块
     */
    @DeleteMapping("/chunk/{id}")
    public Result<Boolean> deleteChunk(@PathVariable Long id) {
        return Result.success(knowledgeChunkService.removeById(id));
    }

    /**
     * 删除指定文档的所有分块
     */
    @DeleteMapping("/doc/{docTitle}")
    public Result<Boolean> deleteDoc(@PathVariable String docTitle) {
        return Result.success(knowledgeChunkService.deleteByDocTitle(docTitle));
    }
}
