package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "登录认证")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "请求参数不合法"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(Result.success(200, "登录成功", loginResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    @SecurityRequirement(name = "satoken")
    public ResponseEntity<Result<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(Result.success(200, "退出成功", null));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    @SecurityRequirement(name = "satoken")
    public ResponseEntity<Result<CurrentUserResponse>> me() {
        return ResponseEntity.ok(Result.success(authService.getCurrentUser()));
    }

    @GetMapping("/is-login")
    @Operation(summary = "检查登录状态")
    @SecurityRequirement(name = "satoken")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public ResponseEntity<Result<LoginStatusResponse>> isLogin() {
        return ResponseEntity.ok(Result.success(authService.getLoginStatus()));
    }
}
