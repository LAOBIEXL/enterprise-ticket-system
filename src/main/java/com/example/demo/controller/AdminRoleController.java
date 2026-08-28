package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateRoleRequest;
import com.example.demo.dto.ReplaceRolePermissionsRequest;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.dto.UpdateRoleRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.service.RolePermissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/admin/roles")
@Tag(name = "角色管理")
@SecurityRequirement(name = "satoken")
@SaCheckPermission("role:manage")
public class AdminRoleController {

    private final RolePermissionAdminService roleService;

    public AdminRoleController(RolePermissionAdminService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "查询角色")
    public ResponseEntity<Result<List<RoleAdminResponse>>> roles() {
        return ResponseEntity.ok(Result.success(roleService.getRoles()));
    }

    @PostMapping
    @Operation(summary = "创建角色")
    public ResponseEntity<Result<RoleAdminResponse>> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, "角色创建成功", roleService.createRole(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改角色")
    public ResponseEntity<Result<RoleAdminResponse>> update(
            @Positive(message = "角色 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(Result.success(roleService.updateRole(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或停用角色")
    public ResponseEntity<Result<RoleAdminResponse>> updateStatus(
            @Positive(message = "角色 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(Result.success(roleService.updateRoleStatus(id, request)));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "替换角色权限")
    public ResponseEntity<Result<RoleAdminResponse>> replacePermissions(
            @Positive(message = "角色 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        return ResponseEntity.ok(Result.success(roleService.replacePermissions(id, request)));
    }
}
