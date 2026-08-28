package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LoginUserResponse", description = "登录用户摘要")
public record LoginUserResponse(
        String id,
        String username,
        String name,
        String departmentName,
        List<String> roles
) {
}
