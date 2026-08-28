package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateTicketCategoryRequest;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.service.TicketCategoryAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTicketCategoryControllerTests {

    @Test
    void shouldRequireCategoryManagePermissionForController() {
        SaCheckPermission permission = AdminTicketCategoryController.class
                .getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly("ticket:category:manage");
    }

    @Test
    void shouldReturnCreatedForNewCategory() {
        TicketCategoryAdminService service = mock(TicketCategoryAdminService.class);
        AdminTicketCategoryController controller = new AdminTicketCategoryController(service);
        CreateTicketCategoryRequest request = new CreateTicketCategoryRequest(
                "HARDWARE", "硬件故障", null, 10
        );
        TicketCategoryResponse expected = new TicketCategoryResponse(
                "9", "HARDWARE", "硬件故障", null, 1, 10, null, null
        );
        when(service.create(request)).thenReturn(expected);

        ResponseEntity<Result<TicketCategoryResponse>> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(expected);
        verify(service).create(request);
    }
}
