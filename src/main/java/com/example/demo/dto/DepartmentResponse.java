package com.example.demo.dto;

import java.time.LocalDateTime;

public record DepartmentResponse(
        String id,
        String code,
        String name,
        Integer status,
        Integer sortOrder,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
