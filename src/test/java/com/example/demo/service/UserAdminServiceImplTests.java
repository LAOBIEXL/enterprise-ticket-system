package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo.converter.UserAdminConverter;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.ReplaceUserRolesRequest;
import com.example.demo.dto.ResetUserPasswordRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UserAdminResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysRole;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

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

class UserAdminServiceImplTests {

    private SysUserMapper userMapper;
    private SysDepartmentMapper departmentMapper;
    private SysRoleMapper roleMapper;
    private PasswordEncoder passwordEncoder;
    private UserAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        departmentMapper = mock(SysDepartmentMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserAdminServiceImpl(
                userMapper, departmentMapper, roleMapper, passwordEncoder, new UserAdminConverter()
        );
    }

    @Test
    void shouldCreateUserWithEncodedPasswordAndNormalizedProfile() {
        when(departmentMapper.selectById(5L)).thenReturn(department(5L, 1));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("LongPassword12")).thenReturn("bcrypt-hash");
        doAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(8L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));
        when(userMapper.selectById(8L)).thenReturn(user(8L, 5L, "alice"));
        when(userMapper.selectRolesByUserId(8L)).thenReturn(List.of());

        UserAdminResponse response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(1L);
            response = service.create(new CreateUserRequest(
                    5L, " alice ", "LongPassword12", " Alice ", " ALICE@example.com ", " 13800000000 "
            ));
        }

        assertThat(response.username()).isEqualTo("alice");
        verify(userMapper).insert(org.mockito.ArgumentMatchers.<SysUser>argThat(user ->
                "bcrypt-hash".equals(user.getPasswordHash())
                        && "alice@example.com".equals(user.getEmail())
                        && "Alice".equals(user.getName())
                        && Long.valueOf(1L).equals(user.getCreateBy())
        ));
    }

    @Test
    void shouldRejectDisabledDepartmentWhenCreatingUser() {
        when(departmentMapper.selectById(5L)).thenReturn(department(5L, 0));

        assertThatThrownBy(() -> service.create(new CreateUserRequest(
                5L, "alice", "LongPassword12", "Alice", null, null
        ))).isInstanceOf(BusinessConflictException.class)
                .hasMessage("部门不存在或已停用");

        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void shouldRejectDisablingCurrentAccount() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, 5L, "admin"));

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            assertThatThrownBy(() -> service.updateStatus(7L, new UpdateStatusRequest(0)))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("不能停用当前登录账号");
        }

        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void shouldReplaceRolesAtomicallyAndInvalidateTargetSessions() {
        when(userMapper.selectById(8L)).thenReturn(user(8L, 5L, "alice"));
        when(departmentMapper.selectById(5L)).thenReturn(department(5L, 1));
        when(userMapper.selectRolesByUserId(8L)).thenReturn(List.of());
        when(roleMapper.selectBatchIds(Set.of(2L, 3L)))
                .thenReturn(List.of(role(2L, 1), role(3L, 1)));

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            service.replaceRoles(8L, new ReplaceUserRolesRequest(Set.of(2L, 3L)));
            stp.verify(() -> StpUtil.kickout(8L));
        }

        verify(userMapper).deleteUserRoles(8L);
        verify(userMapper).insertUserRoleWithCreator(8L, 2L, 7L);
        verify(userMapper).insertUserRoleWithCreator(8L, 3L, 7L);
    }

    @Test
    void shouldHashResetPasswordAndInvalidateSessions() {
        when(userMapper.selectById(8L)).thenReturn(user(8L, 5L, "alice"));
        when(passwordEncoder.encode("AnotherPass12")).thenReturn("new-hash");

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            service.resetPassword(8L, new ResetUserPasswordRequest("AnotherPass12"));
            stp.verify(() -> StpUtil.kickout(8L));
        }

        verify(passwordEncoder).encode("AnotherPass12");
        verify(userMapper).updateById(org.mockito.ArgumentMatchers.<SysUser>argThat(user ->
                "new-hash".equals(user.getPasswordHash())
                        && Long.valueOf(7L).equals(user.getUpdateBy())
        ));
    }

    private SysDepartment department(Long id, Integer status) {
        SysDepartment department = new SysDepartment();
        department.setId(id);
        department.setName("技术部");
        department.setStatus(status);
        return department;
    }

    private SysUser user(Long id, Long departmentId, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDepartmentId(departmentId);
        user.setUsername(username);
        user.setName("用户");
        user.setStatus(1);
        return user;
    }

    private SysRole role(Long id, Integer status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        return role;
    }
}
