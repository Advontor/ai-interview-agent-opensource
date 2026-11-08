package com.nanji.interview.ai_interview_system.modules.resume.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.resume.entity.Resume;
import com.nanji.interview.ai_interview_system.modules.resume.mapper.ResumeMapper;
import com.nanji.interview.ai_interview_system.modules.resume.service.ResumeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {

    @Override
    public List<Resume> listByUserId(Long userId) {
        return lambdaQuery()
                .eq(Resume::getUserId, userId)
                .list();
    }
}
