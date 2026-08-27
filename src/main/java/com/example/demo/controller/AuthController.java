package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "简易登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "用户 ID 不合法"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Result<LoginResponse>> login(@RequestBody LoginRequest request) {
        if (request == null || request.userId() == null || request.userId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Result.fail(400, "用户 ID 必须是大于 0 的整数", null));
        }

        LoginResponse loginResponse = authService.login(request.userId());
        if (loginResponse == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail(404, "用户不存在", null));
        }
        return ResponseEntity.ok(Result.success(200, "登录成功", loginResponse));
    }

    @GetMapping("/is-login")
    @Operation(summary = "检查登录状态")
    @SecurityRequirement(name = "satoken")
    @ApiResponse(responseCode = "200", description = "检查成功")
    public ResponseEntity<Result<LoginStatusResponse>> isLogin() {
        return ResponseEntity.ok(Result.success(authService.getLoginStatus()));
    }
}
