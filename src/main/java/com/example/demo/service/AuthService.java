package com.example.demo.service;

import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;

/**
 * 登录认证业务接口。
 */
public interface AuthService {

    /**
     * 根据用户 ID 创建登录会话；用户不存在时返回 null。
     */
    LoginResponse login(Long userId);

    /**
     * 获取当前请求携带 Token 的登录状态。
     */
    LoginStatusResponse getLoginStatus();
}
