package com.nanji.interview.ai_interview_system.modules.interview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.interview.entity.InterviewMessage;
import com.nanji.interview.ai_interview_system.modules.interview.mapper.InterviewMessageMapper;
import com.nanji.interview.ai_interview_system.modules.interview.service.InterviewMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewMessageServiceImpl extends ServiceImpl<InterviewMessageMapper, InterviewMessage>
        implements InterviewMessageService {

    @Override
    public List<InterviewMessage> listBySessionId(Long sessionId) {
        return lambdaQuery()
                .eq(InterviewMessage::getSessionId, sessionId)
                .orderByAsc(InterviewMessage::getCreateTime)
                .orderByAsc(InterviewMessage::getId)
                .list();
    }
}
