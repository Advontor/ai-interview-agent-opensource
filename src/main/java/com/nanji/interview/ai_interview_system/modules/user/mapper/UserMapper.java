package com.nanji.interview.ai_interview_system.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nanji.interview.ai_interview_system.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
