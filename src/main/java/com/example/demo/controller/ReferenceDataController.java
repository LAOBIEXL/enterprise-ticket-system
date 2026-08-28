package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "基础数据")
@SecurityRequirement(name = "satoken")
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    public ReferenceDataController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping("/departments")
    @Operation(summary = "查询启用的部门")
    public ResponseEntity<Result<List<ReferenceItemResponse>>> departments() {
        return ResponseEntity.ok(Result.success(referenceDataService.getEnabledDepartments()));
    }

    @GetMapping("/ticket-categories")
    @Operation(summary = "查询启用的工单分类")
    public ResponseEntity<Result<List<ReferenceItemResponse>>> ticketCategories() {
        return ResponseEntity.ok(Result.success(referenceDataService.getEnabledTicketCategories()));
    }
}
