package com.example.demo.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentQuery(
        @Size(max = 100, message = "关键词长度不能超过 100 个字符") String keyword,
        @Pattern(regexp = "0|1", message = "状态只能是 0 或 1") String status,
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

    @Parameter(hidden = true)
    public Integer normalizedStatus() {
        return status == null || status.isBlank() ? null : Integer.valueOf(status);
    }
}
