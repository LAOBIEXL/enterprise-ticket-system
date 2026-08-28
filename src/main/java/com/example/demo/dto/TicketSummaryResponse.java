package com.example.demo.dto;

import java.time.LocalDateTime;

public record TicketSummaryResponse(
        String id,
        String ticketNo,
        String title,
        String status,
        String categoryId,
        String categoryName,
        String requesterId,
        String requesterName,
        String requesterDepartmentName,
        String assigneeId,
        String assigneeName,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
