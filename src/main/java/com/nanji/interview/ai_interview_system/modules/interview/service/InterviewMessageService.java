package com.nanji.interview.ai_interview_system.modules.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewMessage;

import java.util.List;

public interface InterviewMessageService extends IService<InterviewMessage> {

    /** 根据会话 id 按时间顺序查询全部对话记录 */
    List<InterviewMessage> listBySessionId(Long sessionId);
}
