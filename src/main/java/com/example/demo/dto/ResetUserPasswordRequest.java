package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 12, max = 64, message = "新密码长度必须为 12 到 64 个字符") String newPassword
) {
}
