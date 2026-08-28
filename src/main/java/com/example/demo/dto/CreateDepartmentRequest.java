package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "部门编码不能为空")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,49}", message = "部门编码格式不合法") String code,
        @NotBlank(message = "部门名称不能为空")
        @Size(max = 100, message = "部门名称长度不能超过 100 个字符") String name,
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于 0")
        @Max(value = 100000, message = "排序值不能超过 100000") Integer sortOrder
) {
}
