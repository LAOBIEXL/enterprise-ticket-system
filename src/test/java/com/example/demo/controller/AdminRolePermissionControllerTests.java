package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateRoleRequest;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.service.RolePermissionAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminRolePermissionControllerTests {

    @Test
    void shouldDeclareSeparateRoleAndPermissionManagementPermissions() {
        SaCheckPermission rolePermission = AdminRoleController.class.getAnnotation(SaCheckPermission.class);
        SaCheckPermission permissionPermission = AdminPermissionController.class
                .getAnnotation(SaCheckPermission.class);

        assertThat(rolePermission.value()).containsExactly("role:manage");
        assertThat(permissionPermission.value()).containsExactly("permission:manage");
    }

    @Test
    void shouldReturnCreatedForNewRole() {
        RolePermissionAdminService service = mock(RolePermissionAdminService.class);
        AdminRoleController controller = new AdminRoleController(service);
        CreateRoleRequest request = new CreateRoleRequest("AUDITOR", "审计员", null);
        RoleAdminResponse expected = new RoleAdminResponse(
                "9", "AUDITOR", "审计员", null, 1, List.of(), null, null
        );
        when(service.createRole(request)).thenReturn(expected);

        ResponseEntity<Result<RoleAdminResponse>> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(expected);
    }
}
