package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateTicketRequest", description = "创建工单请求")
public record CreateTicketRequest(
        @NotNull(message = "工单分类不能为空")
        @Positive(message = "工单分类 ID 必须大于 0")
        Long categoryId,

        @NotBlank(message = "工单标题不能为空")
        @Size(max = 200, message = "工单标题长度不能超过 200 个字符")
        String title,

        @NotBlank(message = "问题描述不能为空")
        @Size(max = 5000, message = "问题描述长度不能超过 5000 个字符")
        String description
) {
}
