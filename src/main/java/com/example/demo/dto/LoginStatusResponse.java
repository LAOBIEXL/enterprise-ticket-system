package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginStatusResponse", description = "当前请求的登录状态")
public record LoginStatusResponse(
        @Schema(description = "当前请求是否已经登录", example = "true")
        boolean loggedIn,
        @Schema(description = "当前登录 ID；未登录时为 null", example = "1")
        String loginId,
        @Schema(description = "Token 请求头名称", example = "satoken")
        String tokenName,
        @Schema(description = "Token 剩余有效时间；未登录时为 null", example = "2591900")
        Long tokenTimeout
) {
}
