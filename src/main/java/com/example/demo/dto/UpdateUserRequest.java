package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotNull(message = "部门 ID 不能为空") @Positive(message = "部门 ID 必须大于 0") Long departmentId,
        @NotBlank(message = "姓名不能为空") @Size(max = 100, message = "姓名长度不能超过 100 个字符") String name,
        @Email(message = "邮箱格式不合法") @Size(max = 128, message = "邮箱长度不能超过 128 个字符") String email,
        @Size(max = 32, message = "手机号长度不能超过 32 个字符") String mobile
) {
}
