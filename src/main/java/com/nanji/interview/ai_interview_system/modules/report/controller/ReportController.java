package com.nanji.interview.ai_interview_system.modules.report.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.report.entity.Report;
import com.nanji.interview.ai_interview_system.modules.report.service.InterviewReportPdfService;
import com.nanji.interview.ai_interview_system.modules.report.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 报告模块统一接口：
 *  - GET /report/session/{sessionId}：查询面试评价（报告页数据源）
 *  - GET /report/pdf/{sessionId}：导出 PDF 面试报告
 *  - POST /report：保存报告（沿用 develop 原有能力；正常流程由结束面试自动保存）
 */
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    @Resource
    private InterviewReportPdfService interviewReportPdfService;

    // 保存面试报告（面试结束后AI生成报告调用）
    @PostMapping
    public Result<Boolean> saveReport(@RequestBody Report report) {
        return Result.success(reportService.save(report));
    }

    // 根据会话sessionId查询对应的面试报告评价
    @GetMapping("/session/{sessionId}")
    public Result<InterviewEvaluation> getReportBySessionId(@PathVariable Long sessionId) {
        return Result.success(reportService.getEvaluationBySessionId(sessionId));
    }

    // 导出 PDF 面试报告
    @GetMapping("/pdf/{sessionId}")
    public ResponseEntity<byte[]> exportReportPdf(@PathVariable Long sessionId) {
        byte[] pdf = interviewReportPdfService.generateReportPdf(sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "interview-report-" + sessionId + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
