package com.example.demo.security;

import cn.dev33.satoken.stp.StpInterface;
import com.example.demo.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token RBAC 角色与权限数据提供器。
 *
 * <p>角色和权限均从数据库读取，停用用户、角色或权限不会被授予权限。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    public StpInterfaceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        return userId == null ? List.of() : sysUserMapper.selectEnabledPermissionCodesByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        return userId == null ? List.of() : sysUserMapper.selectEnabledRoleCodesByUserId(userId);
    }

    private Long parseUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
