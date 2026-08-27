package com.example.demo.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StpInterfaceImplTests {

    private final StpInterfaceImpl stpInterface = new StpInterfaceImpl();

    @Test
    void shouldGiveAdministratorAllUserPermissions() {
        assertThat(stpInterface.getRoleList(1L, "login"))
                .containsExactly(StpInterfaceImpl.ROLE_ADMIN);
        assertThat(stpInterface.getPermissionList(1L, "login"))
                .containsExactlyInAnyOrder(
                        StpInterfaceImpl.USER_LIST,
                        StpInterfaceImpl.USER_GET,
                        StpInterfaceImpl.USER_ADD,
                        StpInterfaceImpl.USER_UPDATE,
                        StpInterfaceImpl.USER_DELETE
                );
    }

    @Test
    void shouldGiveRegularUserReadOnlyPermissions() {
        assertThat(stpInterface.getRoleList(2L, "login"))
                .containsExactly(StpInterfaceImpl.ROLE_USER);
        assertThat(stpInterface.getPermissionList(2L, "login"))
                .containsExactly(StpInterfaceImpl.USER_LIST, StpInterfaceImpl.USER_GET);
    }
}
