package com.example.demo.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TicketQuery(
        @Pattern(
                regexp = "PENDING|ASSIGNED|PROCESSING|WAIT_CONFIRM|CLOSED",
                message = "工单状态不合法"
        )
        String status,
        @Positive(message = "分类 ID 必须大于 0") Long categoryId,
        @Positive(message = "提交人 ID 必须大于 0") Long requesterId,
        @Positive(message = "处理人 ID 必须大于 0") Long assigneeId,
        @Size(max = 100, message = "关键词长度不能超过 100 个字符") String keyword,
        @Min(value = 1, message = "页码不能小于 1") Integer pageNum,
        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 100, message = "每页数量不能超过 100") Integer pageSize
) {
    @Parameter(hidden = true)
    public int normalizedPageNum() {
        return pageNum == null ? 1 : pageNum;
    }

    @Parameter(hidden = true)
    public int normalizedPageSize() {
        return pageSize == null ? 10 : pageSize;
    }
}
