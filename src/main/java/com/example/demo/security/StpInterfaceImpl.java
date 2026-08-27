package com.example.demo.security;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token RBAC 角色与权限数据提供器。
 *
 * <p>当前项目尚未建立 RBAC 数据表，因此使用教学用规则：用户 1 为管理员，
 * 其他用户为普通用户。后续接入角色表后，只需替换本类的查询实现。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    public static final String USER_LIST = "user:list";
    public static final String USER_GET = "user:get";
    public static final String USER_ADD = "user:add";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";

    private static final List<String> ADMIN_PERMISSIONS = List.of(
            USER_LIST,
            USER_GET,
            USER_ADD,
            USER_UPDATE,
            USER_DELETE
    );
    private static final List<String> USER_PERMISSIONS = List.of(USER_LIST, USER_GET);

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return isAdmin(loginId) ? ADMIN_PERMISSIONS : USER_PERMISSIONS;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return isAdmin(loginId) ? List.of(ROLE_ADMIN) : List.of(ROLE_USER);
    }

    private boolean isAdmin(Object loginId) {
        return "1".equals(String.valueOf(loginId));
    }
}
