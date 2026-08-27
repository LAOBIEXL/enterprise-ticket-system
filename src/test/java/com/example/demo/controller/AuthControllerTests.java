package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.dto.LoginUserResponse;
import com.example.demo.exception.InvalidCredentialsException;
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
        LoginRequest request = new LoginRequest("admin", "secret-password");
        LoginResponse expected = new LoginResponse(
                "satoken",
                "test-token",
                new LoginUserResponse("1", "admin", "系统管理员", "技术部", java.util.List.of("SYSTEM_ADMIN")),
                2_592_000L
        );
        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<Result<LoginResponse>> httpResponse = authController.login(request);
        Result<LoginResponse> result = httpResponse.getBody();

        assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("登录成功");
        assertThat(result.getData()).isEqualTo(expected);
        verify(authService).login(request);
    }

    @Test
    void shouldKeepCredentialFailureGeneric() {
        LoginRequest request = new LoginRequest("missing", "wrong-password");
        when(authService.login(request)).thenThrow(new InvalidCredentialsException());

        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authController.login(request)
        );
    }

    @Test
    void shouldNotExposeUnknownUserAsNotFound() {
        LoginRequest request = new LoginRequest("missing", "wrong-password");
        when(authService.login(request)).thenThrow(new InvalidCredentialsException());

        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authController.login(request)
        );
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
