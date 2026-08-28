package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public record ConfirmTicketRequest(
        @Size(max = 500, message = "确认备注长度不能超过 500 个字符") String remark
) {
}
