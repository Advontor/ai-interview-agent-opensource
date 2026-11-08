package com.nanji.interview.ai_interview_system.modules.user.controller;

import com.nanji.interview.ai_interview_system.common.Result;
import com.nanji.interview.ai_interview_system.modules.user.entity.User;
import com.nanji.interview.ai_interview_system.modules.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // 用户注册
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        User existing = userService.getByUsername(user.getUsername());
        if (existing != null) {
            return Result.error(400, "用户名已存在");
        }
        return Result.success(userService.save(user));
    }

    // 用户登录
    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        User dbUser = userService.getByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        dbUser.setPassword(null);
        return Result.success(dbUser);
    }

    // 根据id查询用户信息
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
