package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.PageResponse;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateDepartmentRequest;
import com.example.demo.dto.DepartmentQuery;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.dto.UpdateDepartmentRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.service.DepartmentAdminService;
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
@RequestMapping("/admin/departments")
@Tag(name = "部门管理")
@SecurityRequirement(name = "satoken")
@SaCheckPermission("department:manage")
public class AdminDepartmentController {

    private final DepartmentAdminService departmentService;

    public AdminDepartmentController(DepartmentAdminService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "分页查询部门")
    public ResponseEntity<Result<PageResponse<DepartmentResponse>>> page(
            @Valid @ParameterObject DepartmentQuery query) {
        return ResponseEntity.ok(Result.success(departmentService.page(query)));
    }

    @PostMapping
    @Operation(summary = "新增部门")
    public ResponseEntity<Result<DepartmentResponse>> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, "部门创建成功", departmentService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改部门")
    public ResponseEntity<Result<DepartmentResponse>> update(
            @Positive(message = "部门 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(Result.success(departmentService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或停用部门")
    public ResponseEntity<Result<DepartmentResponse>> updateStatus(
            @Positive(message = "部门 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(Result.success(departmentService.updateStatus(id, request)));
    }
}
