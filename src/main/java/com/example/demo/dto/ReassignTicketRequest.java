package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReassignTicketRequest(
        @NotNull(message = "处理人不能为空")
        @Positive(message = "处理人 ID 必须大于 0") Long assigneeId,
        @NotBlank(message = "改派原因不能为空")
        @Size(max = 500, message = "改派原因长度不能超过 500 个字符") String reason
) {
}
