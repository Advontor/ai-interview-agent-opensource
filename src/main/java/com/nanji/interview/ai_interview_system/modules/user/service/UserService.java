package com.nanji.interview.ai_interview_system.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nanji.interview.ai_interview_system.modules.user.entity.User;

public interface UserService extends IService<User> {

    /** 根据用户名查询用户（登录用） */
    User getByUsername(String username);
}
