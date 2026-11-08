package com.nanji.interview.ai_interview_system.modules.learning.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.learning.entity.LearningResource;
import com.nanji.interview.ai_interview_system.modules.learning.service.LearningResourceService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习资源模块统一接口：
 *  - 学习资源 CRUD
 *  - 按分类查询
 *  - 按关联技能标签查询（Agent 根据面试薄弱点推送学习资源用）
 */
@RestController
@RequestMapping("/learning")
public class LearningResourceController {

    @Resource
    private LearningResourceService learningResourceService;

    // 新增学习资源
    @PostMapping
    public Result<Boolean> addResource(@RequestBody LearningResource resource) {
        return Result.success(learningResourceService.save(resource));
    }

    // 根据id查询学习资源
    @GetMapping("/{id}")
    public Result<LearningResource> getResource(@PathVariable Long id) {
        return Result.success(learningResourceService.getById(id));
    }

    // 查询全部学习资源
    @GetMapping("/list")
    public Result<List<LearningResource>> getAllResources() {
        return Result.success(learningResourceService.list());
    }

    // 根据分类查询学习资源
    @GetMapping("/category/{category}")
    public Result<List<LearningResource>> getResourcesByCategory(@PathVariable String category) {
        return Result.success(learningResourceService.listByCategory(category));
    }

    // 根据关联技能标签查询学习资源（匹配面试薄弱点）
    @GetMapping("/skill/{relatedSkill}")
    public Result<List<LearningResource>> getResourcesBySkill(@PathVariable String relatedSkill) {
        return Result.success(learningResourceService.listByRelatedSkill(relatedSkill));
    }

    // 更新学习资源
    @PutMapping
    public Result<Boolean> updateResource(@RequestBody LearningResource resource) {
        return Result.success(learningResourceService.updateById(resource));
    }

    // 删除学习资源
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteResource(@PathVariable Long id) {
        return Result.success(learningResourceService.removeById(id));
    }
}
