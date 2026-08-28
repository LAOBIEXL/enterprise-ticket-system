package com.example.demo.security;

import com.example.demo.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StpInterfaceImplTests {

    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final StpInterfaceImpl stpInterface = new StpInterfaceImpl(sysUserMapper);

    @Test
    void shouldLoadRolesAndPermissionsFromDatabase() {
        when(sysUserMapper.selectEnabledRoleCodesByUserId(1L))
                .thenReturn(List.of("SYSTEM_ADMIN"));
        when(sysUserMapper.selectEnabledPermissionCodesByUserId(1L))
                .thenReturn(List.of("user:manage"));

        assertThat(stpInterface.getRoleList(1L, "login"))
                .containsExactly("SYSTEM_ADMIN");
        assertThat(stpInterface.getPermissionList(1L, "login"))
                .containsExactly("user:manage");
    }

    @Test
    void shouldReturnNoAuthoritiesForInvalidLoginId() {
        assertThat(stpInterface.getRoleList("not-a-user-id", "login")).isEmpty();
        assertThat(stpInterface.getPermissionList(null, "login")).isEmpty();
    }
}
