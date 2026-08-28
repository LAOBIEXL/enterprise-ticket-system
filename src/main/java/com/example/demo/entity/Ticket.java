package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private Long categoryId;
    private Long requesterId;
    private Long requesterDepartmentId;
    private Long assigneeId;
    private String status;
    private Integer version;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
