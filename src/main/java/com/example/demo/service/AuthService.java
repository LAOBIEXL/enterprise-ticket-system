package com.example.demo.service;

import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;

/**
 * 登录认证业务接口。
 */
public interface AuthService {

    /**
     * 校验账号密码并创建登录会话。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 注销当前请求携带的 Token。
     */
    void logout();

    /**
     * 获取当前登录用户、角色和权限。
     */
    CurrentUserResponse getCurrentUser();

    /**
     * 获取当前请求携带 Token 的登录状态。
     */
    LoginStatusResponse getLoginStatus();
}
