package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "简易登录请求；当前项目尚无密码字段，仅用于演示 Sa-Token 登录流程")
public record LoginRequest(
        @Schema(description = "要登录的用户 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId
) {
}
