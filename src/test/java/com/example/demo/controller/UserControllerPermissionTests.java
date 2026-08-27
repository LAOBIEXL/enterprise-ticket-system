package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerPermissionTests {

    @Test
    void shouldDeclareCrudPermissionsOnUserEndpoints() throws NoSuchMethodException {
        assertPermission("save", "user:add", User.class);
        assertPermission("getAll", "user:list");
        assertPermission("getById", "user:get", Long.class);
        assertPermission("update", "user:update", Long.class, User.class);
        assertPermission("delete", "user:delete", Long.class);
        assertPermission("getPage", "user:list", long.class, long.class, String.class);
    }

    private void assertPermission(String methodName, String permission, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        SaCheckPermission annotation = UserController.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(SaCheckPermission.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }
}
