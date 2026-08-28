package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserAdminResponse(
        String id,
        String departmentId,
        String departmentName,
        String username,
        String name,
        String email,
        String mobile,
        Integer status,
        List<ReferenceItemResponse> roles,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
