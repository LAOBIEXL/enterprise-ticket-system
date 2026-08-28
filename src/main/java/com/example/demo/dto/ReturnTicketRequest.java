package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnTicketRequest(
        @NotBlank(message = "退回原因不能为空")
        @Size(max = 1000, message = "退回原因长度不能超过 1000 个字符") String reason
) {
}
