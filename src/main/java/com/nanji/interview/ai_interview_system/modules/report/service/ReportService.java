package com.nanji.interview.ai_interview_system.modules.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.interview.dto.InterviewEvaluation;
import com.nanji.interview.ai_interview_system.modules.report.entity.Report;

import java.util.List;
import java.util.Map;

public interface ReportService extends IService<Report> {

    /** 保存面试评价（结束面试时调用） */
    void saveEvaluation(Long sessionId, InterviewEvaluation evaluation);

    /** 根据会话 id 查询面试评价（统一返回 InterviewEvaluation 结构） */
    InterviewEvaluation getEvaluationBySessionId(Long sessionId);

    /** 删除某场面试的报告（随面试记录一并删除） */
    void deleteBySessionId(Long sessionId);

    /**
     * 批量查询多场面试的报告得分（历史列表用）。
     *
     * @param sessionIds 会话 id 列表
     * @return sessionId -> 得分；未生成报告或无得分的会话不会出现在结果里
     */
    Map<Long, Integer> findScoresBySessionIds(List<Long> sessionIds);
}
