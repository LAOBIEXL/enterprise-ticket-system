package com.example.demo.mapper.model;

import lombok.Data;

import java.time.LocalDateTime;

/** 工单联合查询的持久层只读投影。 */
@Data
public class TicketViewRow {
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private String status;
    private Long categoryId;
    private String categoryName;
    private Long requesterId;
    private String requesterName;
    private String requesterDepartmentName;
    private Long assigneeId;
    private String assigneeName;
    private Integer version;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
