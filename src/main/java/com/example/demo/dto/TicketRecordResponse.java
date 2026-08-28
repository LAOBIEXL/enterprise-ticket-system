package com.example.demo.dto;

import java.time.LocalDateTime;

public record TicketRecordResponse(
        String id,
        String action,
        String fromStatus,
        String toStatus,
        String content,
        String operatorId,
        String operatorName,
        String targetUserId,
        String targetUserName,
        LocalDateTime createTime
) {
}
