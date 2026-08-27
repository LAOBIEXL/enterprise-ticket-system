package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CurrentUserResponse", description = "当前登录用户及其 RBAC 信息")
public record CurrentUserResponse(
        String id,
        String username,
        String name,
        String departmentName,
        List<String> roles,
        List<String> permissions
) {
}
