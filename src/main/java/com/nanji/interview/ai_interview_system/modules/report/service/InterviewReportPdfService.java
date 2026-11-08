package com.nanji.interview.ai_interview_system.modules.report.service;

/**
 * PDF 面试报告生成服务（迁移自 feature/interview-agent，数据改为从数据库读取）。
 */
public interface InterviewReportPdfService {

    /**
     * 根据一次已完成的面试会话生成 PDF 面试报告。
     *
     * @param sessionId 面试会话 id
     * @return PDF 文件字节
     */
    byte[] generateReportPdf(Long sessionId);
}
