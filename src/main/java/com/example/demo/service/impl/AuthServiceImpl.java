package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.converter.AuthConverter;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 Sa-Token 的登录认证业务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final int ENABLED = 1;

    private final SysUserMapper sysUserMapper;
    private final AuthConverter authConverter;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            AuthConverter authConverter,
            PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.authConverter = authConverter;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectByUsername(request.username());
        if (user == null
                || !Integer.valueOf(ENABLED).equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        StpUtil.login(user.getId());
        List<String> roles = sysUserMapper.selectEnabledRoleCodesByUserId(user.getId());
        return authConverter.toLoginResponse(
                user,
                sysUserMapper.selectEnabledDepartmentNameById(user.getDepartmentId()),
                roles,
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                StpUtil.getTokenTimeout()
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        StpUtil.checkLogin();
        Long userId = Long.valueOf(String.valueOf(StpUtil.getLoginId()));
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Integer.valueOf(ENABLED).equals(user.getStatus())) {
            StpUtil.logout();
            StpUtil.checkLogin();
        }

        return authConverter.toCurrentUserResponse(
                user,
                sysUserMapper.selectEnabledDepartmentNameById(user.getDepartmentId()),
                sysUserMapper.selectEnabledRoleCodesByUserId(userId),
                sysUserMapper.selectEnabledPermissionCodesByUserId(userId)
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
