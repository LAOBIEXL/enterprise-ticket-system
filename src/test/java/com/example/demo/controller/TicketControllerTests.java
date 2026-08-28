package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketControllerTests {

    @Test
    void shouldReturnCreatedStatusForNewTicket() {
        TicketService service = mock(TicketService.class);
        TicketController controller = new TicketController(service);
        CreateTicketRequest request = new CreateTicketRequest(1L, "无法访问系统", "登录后页面一直加载");
        TicketDetailResponse expected = detail("100");
        String key = "550e8400-e29b-41d4-a716-446655440000";
        when(service.create(request, key)).thenReturn(expected);

        ResponseEntity<Result<TicketDetailResponse>> response = controller.create(key, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getCode()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(expected);
        verify(service).create(request, key);
    }

    @Test
    void shouldUseAnyReadPermissionForDetailEndpoint() throws NoSuchMethodException {
        SaCheckPermission permission = TicketController.class
                .getMethod("detail", Long.class)
                .getAnnotation(SaCheckPermission.class);

        assertThat(permission.value()).containsExactly(
                "ticket:read:own", "ticket:read:assigned", "ticket:read:all"
        );
        assertThat(permission.mode()).isEqualTo(SaMode.OR);
    }

    private TicketDetailResponse detail(String id) {
        return new TicketDetailResponse(
                id, "TK202608280001", "无法访问系统", "登录后页面一直加载", "PENDING",
                "1", "软件问题", "7", "张三", "研发部", null, null,
                0, null, null, null, null, List.of()
        );
    }
}
