package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_record")
public class TicketRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long operatorId;
    private Long targetUserId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String content;
    private LocalDateTime createTime;
}
