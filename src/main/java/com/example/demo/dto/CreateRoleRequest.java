package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,63}", message = "角色编码格式不合法") String code,
        @NotBlank(message = "角色名称不能为空") @Size(max = 100, message = "角色名称长度不能超过 100 个字符") String name,
        @Size(max = 255, message = "角色说明长度不能超过 255 个字符") String description
) {
}
