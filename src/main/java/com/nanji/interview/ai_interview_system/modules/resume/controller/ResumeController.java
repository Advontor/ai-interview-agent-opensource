package com.nanji.interview.ai_interview_system.modules.resume.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.common.exception.BusinessException;
import com.nanji.interview.ai_interview_system.modules.resume.dto.ResumeDTO;
import com.nanji.interview.ai_interview_system.modules.resume.entity.Resume;
import com.nanji.interview.ai_interview_system.modules.resume.parser.ResumeFileExtractor;
import com.nanji.interview.ai_interview_system.modules.resume.service.ResumeParseService;
import com.nanji.interview.ai_interview_system.modules.resume.service.ResumeService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 简历模块统一接口：
 *  - 上传文件（PDF/Word）并解析为 ResumeDTO（第一周主流程入口）
 *  - 简历纯文本解析（联调/调试用）
 *  - 简历基础 CRUD（沿用 develop 原有能力）
 *
 * 注：原 POST /resume/parsed 与 POST /resume 等价（都是保存简历），已合并删除。
 */
@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Resource
    private ResumeService resumeService;

    @Resource
    private ResumeParseService resumeParseService;

    /**
     * 上传简历文件（.pdf / .docx），提取纯文字并解析为 ResumeDTO。
     */
    @PostMapping(value = "/upload", produces = "application/json;charset=UTF-8")
    public Result<ResumeDTO> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "resume.pdf";
        String text;
        try {
            text = ResumeFileExtractor.extractText(fileName, file.getBytes());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        } catch (IOException e) {
            throw new BusinessException("简历文件读取失败：" + e.getMessage());
        }
        if (text == null || text.isBlank()) {
            throw new BusinessException("未能从简历文件中提取到文字（扫描件 PDF 暂不支持）");
        }
        return Result.success(resumeParseService.parse(text));
    }

    /**
     * 直接提交简历纯文本进行解析（联调/调试用）。
     */
    @PostMapping(value = "/parse", produces = "application/json;charset=UTF-8")
    public Result<ResumeDTO> parseResume(@RequestBody String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new BusinessException("简历文本不能为空");
        }
        return Result.success(resumeParseService.parse(resumeText));
    }

    // 新增简历
    @PostMapping
    public Result<Boolean> addResume(@RequestBody Resume resume) {
        return Result.success(resumeService.save(resume));
    }

    // 根据用户id，查询该用户所有简历
    @GetMapping("/list/{userId}")
    public Result<List<Resume>> getResumeList(@PathVariable Long userId) {
        return Result.success(resumeService.listByUserId(userId));
    }

    // 根据简历id单条查询简历详情
    @GetMapping("/{id}")
    public Result<Resume> getResumeDetail(@PathVariable Long id) {
        return Result.success(resumeService.getById(id));
    }

    // 删除简历
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteResume(@PathVariable Long id) {
        return Result.success(resumeService.removeById(id));
    }
}
