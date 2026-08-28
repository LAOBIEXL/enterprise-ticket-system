package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record ReplaceRolePermissionsRequest(
        @NotNull(message = "权限 ID 集合不能为空") Set<@Positive(message = "权限 ID 必须大于 0") Long> permissionIds
) {
}
