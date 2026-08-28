package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateDepartmentRequest;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.service.DepartmentAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDepartmentControllerTests {

    @Test
    void shouldRequireDepartmentManagePermission() {
        SaCheckPermission permission = AdminDepartmentController.class
                .getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly("department:manage");
    }

    @Test
    void shouldReturnCreatedForNewDepartment() {
        DepartmentAdminService service = mock(DepartmentAdminService.class);
        AdminDepartmentController controller = new AdminDepartmentController(service);
        CreateDepartmentRequest request = new CreateDepartmentRequest("SERVICE", "客服部", 60);
        DepartmentResponse expected = new DepartmentResponse(
                "8", "SERVICE", "客服部", 1, 60, null, null
        );
        when(service.create(request)).thenReturn(expected);

        ResponseEntity<Result<DepartmentResponse>> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(expected);
        verify(service).create(request);
    }
}
