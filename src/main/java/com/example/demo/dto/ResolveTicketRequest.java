package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveTicketRequest(
        @NotBlank(message = "解决方案不能为空")
        @Size(max = 5000, message = "解决方案长度不能超过 5000 个字符") String solution
) {
}
