package com.example.demo.mapper.model;

import lombok.Data;

import java.time.LocalDateTime;

/** 工单操作记录联合查询的持久层只读投影。 */
@Data
public class TicketRecordRow {
    private Long id;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String content;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    private LocalDateTime createTime;
}
