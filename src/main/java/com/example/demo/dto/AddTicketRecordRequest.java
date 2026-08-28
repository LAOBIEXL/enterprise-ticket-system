package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTicketRecordRequest(
        @NotBlank(message = "处理记录不能为空")
        @Size(max = 2000, message = "处理记录长度不能超过 2000 个字符") String content
) {
}
