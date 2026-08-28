package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDetailResponse(
        String id,
        String ticketNo,
        String title,
        String description,
        String status,
        String categoryId,
        String categoryName,
        String requesterId,
        String requesterName,
        String requesterDepartmentName,
        String assigneeId,
        String assigneeName,
        int version,
        LocalDateTime resolvedTime,
        LocalDateTime closedTime,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<TicketRecordResponse> records
) {
}
