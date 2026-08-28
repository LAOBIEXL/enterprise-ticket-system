package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.demo.common.PageResponse;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateTicketCategoryRequest;
import com.example.demo.dto.TicketCategoryQuery;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateTicketCategoryRequest;
import com.example.demo.service.TicketCategoryAdminService;
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
@RequestMapping("/admin/ticket-categories")
@Tag(name = "工单分类管理")
@SecurityRequirement(name = "satoken")
@SaCheckPermission("ticket:category:manage")
public class AdminTicketCategoryController {

    private final TicketCategoryAdminService categoryService;

    public AdminTicketCategoryController(TicketCategoryAdminService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "分页查询工单分类")
    public ResponseEntity<Result<PageResponse<TicketCategoryResponse>>> page(
            @Valid @ParameterObject TicketCategoryQuery query) {
        return ResponseEntity.ok(Result.success(categoryService.page(query)));
    }

    @PostMapping
    @Operation(summary = "新增工单分类")
    public ResponseEntity<Result<TicketCategoryResponse>> create(
            @Valid @RequestBody CreateTicketCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, "工单分类创建成功", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改工单分类")
    public ResponseEntity<Result<TicketCategoryResponse>> update(
            @Positive(message = "分类 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateTicketCategoryRequest request) {
        return ResponseEntity.ok(Result.success(categoryService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或停用工单分类")
    public ResponseEntity<Result<TicketCategoryResponse>> updateStatus(
            @Positive(message = "分类 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(Result.success(categoryService.updateStatus(id, request)));
    }
}
