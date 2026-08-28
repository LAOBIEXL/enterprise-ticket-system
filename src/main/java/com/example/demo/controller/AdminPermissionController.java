package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.Result;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.service.RolePermissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/permissions")
@Tag(name = "权限管理")
@SecurityRequirement(name = "satoken")
@SaCheckPermission("permission:manage")
public class AdminPermissionController {

    private final RolePermissionAdminService roleService;

    public AdminPermissionController(RolePermissionAdminService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "查询权限定义")
    public ResponseEntity<Result<List<PermissionResponse>>> permissions() {
        return ResponseEntity.ok(Result.success(roleService.getPermissions()));
    }
}
