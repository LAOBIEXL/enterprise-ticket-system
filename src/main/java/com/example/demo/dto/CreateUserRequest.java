package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotNull(message = "部门 ID 不能为空") @Positive(message = "部门 ID 必须大于 0") Long departmentId,
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]{2,63}", message = "用户名格式不合法") String username,
        @NotBlank(message = "初始密码不能为空")
        @Size(min = 12, max = 64, message = "初始密码长度必须为 12 到 64 个字符") String initialPassword,
        @NotBlank(message = "姓名不能为空") @Size(max = 100, message = "姓名长度不能超过 100 个字符") String name,
        @Email(message = "邮箱格式不合法") @Size(max = 128, message = "邮箱长度不能超过 128 个字符") String email,
        @Size(max = 32, message = "手机号长度不能超过 32 个字符") String mobile
) {
}
