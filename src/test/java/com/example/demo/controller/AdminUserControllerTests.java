package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.UserAdminResponse;
import com.example.demo.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTests {

    @Test
    void shouldRequireUserManagePermission() {
        SaCheckPermission permission = AdminUserController.class.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly("user:manage");
    }

    @Test
    void shouldReturnCreatedForNewUser() {
        UserAdminService service = mock(UserAdminService.class);
        AdminUserController controller = new AdminUserController(service);
        CreateUserRequest request = new CreateUserRequest(
                5L, "alice", "LongPassword12", "Alice", "alice@example.com", null
        );
        UserAdminResponse expected = new UserAdminResponse(
                "8", "5", "技术部", "alice", "Alice", "alice@example.com",
                null, 1, List.of(), null, null
        );
        when(service.create(request)).thenReturn(expected);

        ResponseEntity<Result<UserAdminResponse>> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(expected);
        verify(service).create(request);
    }
}
