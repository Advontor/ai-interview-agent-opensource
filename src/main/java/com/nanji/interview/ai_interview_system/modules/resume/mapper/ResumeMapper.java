package com.nanji.interview.ai_interview_system.modules.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nanji.interview.ai_interview_system.modules.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}