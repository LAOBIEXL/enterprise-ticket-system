package com.example.demo.dto;

import java.time.LocalDateTime;

public record TicketCategoryResponse(
        String id,
        String code,
        String name,
        String description,
        int status,
        int sortOrder,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
