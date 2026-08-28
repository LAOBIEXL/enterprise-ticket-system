package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.PageResponse;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.ReplaceUserRolesRequest;
import com.example.demo.dto.ResetUserPasswordRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserAdminQuery;
import com.example.demo.dto.UserAdminResponse;
import com.example.demo.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/users")
@Tag(name = "用户管理")
@SecurityRequirement(name = "satoken")
@SaCheckPermission("user:manage")
public class AdminUserController {

    private final UserAdminService userService;

    public AdminUserController(UserAdminService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户")
    public ResponseEntity<Result<PageResponse<UserAdminResponse>>> page(
            @Valid @ParameterObject UserAdminQuery query) {
        return ResponseEntity.ok(Result.success(userService.page(query)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public ResponseEntity<Result<UserAdminResponse>> detail(
            @Positive(message = "用户 ID 必须大于 0") @PathVariable Long id) {
        return ResponseEntity.ok(Result.success(userService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public ResponseEntity<Result<UserAdminResponse>> create(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, "用户创建成功", userService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户资料")
    public ResponseEntity<Result<UserAdminResponse>> update(
            @Positive(message = "用户 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(Result.success(userService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或停用用户")
    public ResponseEntity<Result<UserAdminResponse>> updateStatus(
            @Positive(message = "用户 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(Result.success(userService.updateStatus(id, request)));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "替换用户角色")
    public ResponseEntity<Result<UserAdminResponse>> replaceRoles(
            @Positive(message = "用户 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ReplaceUserRolesRequest request) {
        return ResponseEntity.ok(Result.success(userService.replaceRoles(id, request)));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置用户密码")
    public ResponseEntity<Result<Void>> resetPassword(
            @Positive(message = "用户 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ResetUserPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(Result.success(null));
    }
}
