package com.nanji.interview.ai_interview_system.modules.resume.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.resume.entity.Resume;

import java.util.List;

public interface ResumeService extends IService<Resume> {

    /** 根据用户 id 查询该用户所有简历 */
    List<Resume> listByUserId(Long userId);
}
