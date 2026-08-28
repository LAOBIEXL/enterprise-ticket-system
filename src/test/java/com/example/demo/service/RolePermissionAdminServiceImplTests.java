package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo.converter.RoleAdminConverter;
import com.example.demo.dto.CreateRoleRequest;
import com.example.demo.dto.ReplaceRolePermissionsRequest;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.entity.SysPermission;
import com.example.demo.entity.SysRole;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.mapper.SysPermissionMapper;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.service.impl.RolePermissionAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RolePermissionAdminServiceImplTests {

    private SysRoleMapper roleMapper;
    private SysPermissionMapper permissionMapper;
    private RolePermissionAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        roleMapper = mock(SysRoleMapper.class);
        permissionMapper = mock(SysPermissionMapper.class);
        service = new RolePermissionAdminServiceImpl(
                roleMapper, permissionMapper, new RoleAdminConverter()
        );
    }

    @Test
    void shouldCreateEnabledRoleWithNormalizedCode() {
        when(roleMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.<SysRole>getArgument(0).setId(9L);
            return 1;
        }).when(roleMapper).insert(any(SysRole.class));
        when(roleMapper.selectById(9L)).thenReturn(role(9L, "AUDITOR", 1));
        when(roleMapper.selectPermissionsByRoleId(9L)).thenReturn(List.of());

        RoleAdminResponse response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            response = service.createRole(new CreateRoleRequest(" auditor ", " 审计员 ", null));
        }

        assertThat(response.code()).isEqualTo("AUDITOR");
        verify(roleMapper).insert(org.mockito.ArgumentMatchers.<SysRole>argThat(role ->
                "AUDITOR".equals(role.getCode())
                        && "审计员".equals(role.getName())
                        && Long.valueOf(7L).equals(role.getCreateBy())
        ));
    }

    @Test
    void shouldRejectDisablingRoleAssignedToEnabledUsers() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "TECHNICIAN", 1));
        when(roleMapper.countUserRole(7L, 2L)).thenReturn(0L);
        when(roleMapper.countEnabledUsersByRoleId(2L)).thenReturn(3L);

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            assertThatThrownBy(() -> service.updateRoleStatus(2L, new UpdateStatusRequest(0)))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("角色仍分配给启用用户，不能停用");
        }

        verify(roleMapper, never()).updateById(any(SysRole.class));
    }

    @Test
    void shouldRejectChangingPermissionsForCurrentUsersRole() {
        when(roleMapper.selectById(4L)).thenReturn(role(4L, "SYSTEM_ADMIN", 1));
        when(roleMapper.countUserRole(7L, 4L)).thenReturn(1L);

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            assertThatThrownBy(() -> service.replacePermissions(
                    4L, new ReplaceRolePermissionsRequest(Set.of(1L))))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("不能修改当前登录账号所使用角色的授权状态");
        }

        verify(roleMapper, never()).deleteRolePermissions(4L);
    }

    @Test
    void shouldReplacePermissionsAndInvalidateAssignedUsers() {
        when(roleMapper.selectById(3L)).thenReturn(role(3L, "SUPPORT_MANAGER", 1));
        when(roleMapper.countUserRole(7L, 3L)).thenReturn(0L);
        when(permissionMapper.selectBatchIds(Set.of(1L, 2L)))
                .thenReturn(List.of(permission(1L, 1), permission(2L, 1)));
        when(roleMapper.selectUserIdsByRoleId(3L)).thenReturn(List.of(8L, 9L));
        when(roleMapper.selectPermissionsByRoleId(3L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            service.replacePermissions(3L, new ReplaceRolePermissionsRequest(Set.of(1L, 2L)));
            stp.verify(() -> StpUtil.kickout(8L));
            stp.verify(() -> StpUtil.kickout(9L));
        }

        verify(roleMapper).deleteRolePermissions(3L);
        verify(roleMapper).insertRolePermission(3L, 1L, 7L);
        verify(roleMapper).insertRolePermission(3L, 2L, 7L);
    }

    private SysRole role(Long id, String code, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setName("角色");
        role.setStatus(status);
        return role;
    }

    private SysPermission permission(Long id, Integer status) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setStatus(status);
        return permission;
    }
}
