package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoleAdminResponse(
        String id,
        String code,
        String name,
        String description,
        Integer status,
        List<ReferenceItemResponse> permissions,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
