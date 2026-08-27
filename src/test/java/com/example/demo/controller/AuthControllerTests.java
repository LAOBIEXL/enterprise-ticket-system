package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTests {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController authController = new AuthController(authService);

    @Test
    void shouldReturnTokenAfterLogin() {
        LoginResponse expected = new LoginResponse("satoken", "test-token", 1L, 2_592_000L);
        when(authService.login(1L)).thenReturn(expected);

        ResponseEntity<Result<LoginResponse>> httpResponse = authController.login(new LoginRequest(1L));
        Result<LoginResponse> result = httpResponse.getBody();

        assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("登录成功");
        assertThat(result.getData()).isEqualTo(expected);
        verify(authService).login(1L);
    }

    @Test
    void shouldRejectInvalidUserId() {
        ResponseEntity<Result<LoginResponse>> response = authController.login(new LoginRequest(0L));
        Result<LoginResponse> result = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getCode()).isEqualTo(400);
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() {
        when(authService.login(99L)).thenReturn(null);

        ResponseEntity<Result<LoginResponse>> response = authController.login(new LoginRequest(99L));
        Result<LoginResponse> result = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void shouldReturnLoginStatus() {
        LoginStatusResponse status = new LoginStatusResponse(true, "1", "satoken", 100L);
        when(authService.getLoginStatus()).thenReturn(status);

        ResponseEntity<Result<LoginStatusResponse>> response = authController.isLogin();
        Result<LoginStatusResponse> result = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(status);
    }
}
