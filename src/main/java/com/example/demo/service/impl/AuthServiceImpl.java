package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.converter.AuthConverter;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.AuthService;
import org.springframework.stereotype.Service;

/**
 * 基于 Sa-Token 的登录认证业务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AuthConverter authConverter;

    public AuthServiceImpl(UserMapper userMapper, AuthConverter authConverter) {
        this.userMapper = userMapper;
        this.authConverter = authConverter;
    }

    @Override
    public LoginResponse login(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        StpUtil.login(user.getId());
        return authConverter.toLoginResponse(
                user,
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                StpUtil.getTokenTimeout()
        );
    }

    @Override
    public LoginStatusResponse getLoginStatus() {
        boolean loggedIn = StpUtil.isLogin();
        return authConverter.toLoginStatusResponse(
                loggedIn,
                loggedIn ? StpUtil.getLoginId() : null,
                StpUtil.getTokenName(),
                loggedIn ? StpUtil.getTokenTimeout() : null
        );
    }
}
