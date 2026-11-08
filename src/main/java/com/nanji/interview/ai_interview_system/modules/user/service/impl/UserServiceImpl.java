package com.nanji.interview.ai_interview_system.modules.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nanji.interview.ai_interview_system.modules.user.entity.User;
import com.nanji.interview.ai_interview_system.modules.user.mapper.UserMapper;
import com.nanji.interview.ai_interview_system.modules.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }
}
