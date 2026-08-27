package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "登录成功后返回给前端的 Token 信息")
public record LoginResponse(
        @Schema(description = "前端提交 Token 时使用的参数名或请求头名称", example = "satoken")
        String tokenName,
        @Schema(description = "登录凭证；后续请求通过 satoken 请求头携带", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
        String tokenValue,
        @Schema(description = "登录用户摘要")
        LoginUserResponse user,
        @Schema(description = "Token 剩余有效时间，单位为秒", example = "2592000")
        long tokenTimeout
) {
}
