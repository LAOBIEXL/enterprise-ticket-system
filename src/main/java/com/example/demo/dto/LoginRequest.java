package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "账号密码登录请求")
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过 64 个字符")
        @Schema(description = "登录账号", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为 8 到 64 个字符")
        @Schema(description = "登录密码", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}
