package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.example.demo.common.PageResponse;
import com.example.demo.common.Result;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.AddTicketRecordRequest;
import com.example.demo.dto.AssignTicketRequest;
import com.example.demo.dto.ConfirmTicketRequest;
import com.example.demo.dto.ReassignTicketRequest;
import com.example.demo.dto.ResolveTicketRequest;
import com.example.demo.dto.ReturnTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketQuery;
import com.example.demo.dto.TicketSummaryResponse;
import com.example.demo.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/tickets")
@Tag(name = "工单管理")
@SecurityRequirement(name = "satoken")
public class TicketController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @SaCheckPermission("ticket:create")
    @Operation(summary = "创建工单", description = "创建 PENDING 工单并追加 CREATE 操作记录；相同幂等键 24 小时内返回首次结果")
    public ResponseEntity<Result<TicketDetailResponse>> create(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = UUID_PATTERN, message = "Idempotency-Key 必须是标准 UUID") String idempotencyKey,
            @Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse response = ticketService.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(201, "工单创建成功", response));
    }

    @GetMapping("/mine")
    @SaCheckPermission("ticket:read:own")
    @Operation(summary = "分页查询本人提交的工单")
    public ResponseEntity<Result<PageResponse<TicketSummaryResponse>>> mine(
            @Valid @ParameterObject TicketQuery query) {
        return ResponseEntity.ok(Result.success(ticketService.getMine(query)));
    }

    @GetMapping("/assigned")
    @SaCheckPermission("ticket:read:assigned")
    @Operation(summary = "分页查询分派给本人的工单")
    public ResponseEntity<Result<PageResponse<TicketSummaryResponse>>> assigned(
            @Valid @ParameterObject TicketQuery query) {
        return ResponseEntity.ok(Result.success(ticketService.getAssigned(query)));
    }

    @GetMapping
    @SaCheckPermission("ticket:read:all")
    @Operation(summary = "分页查询全部工单")
    public ResponseEntity<Result<PageResponse<TicketSummaryResponse>>> all(
            @Valid @ParameterObject TicketQuery query) {
        return ResponseEntity.ok(Result.success(ticketService.getAll(query)));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(
            value = {"ticket:read:own", "ticket:read:assigned", "ticket:read:all"},
            mode = SaMode.OR
    )
    @Operation(summary = "查询工单详情", description = "除功能权限外，还会校验提交人、当前处理人或全部工单数据范围")
    public ResponseEntity<Result<TicketDetailResponse>> detail(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id) {
        return ResponseEntity.ok(Result.success(ticketService.getDetail(id)));
    }

    @PostMapping("/{id}/assign")
    @SaCheckPermission("ticket:assign")
    @Operation(summary = "分派工单")
    public ResponseEntity<Result<TicketDetailResponse>> assign(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.assign(id, request)));
    }

    @PostMapping("/{id}/reassign")
    @SaCheckPermission("ticket:reassign")
    @Operation(summary = "改派工单")
    public ResponseEntity<Result<TicketDetailResponse>> reassign(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ReassignTicketRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.reassign(id, request)));
    }

    @PostMapping("/{id}/start")
    @SaCheckPermission("ticket:start")
    @Operation(summary = "开始处理工单")
    public ResponseEntity<Result<TicketDetailResponse>> start(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id) {
        return ResponseEntity.ok(Result.success(ticketService.start(id)));
    }

    @PostMapping("/{id}/records")
    @SaCheckPermission("ticket:record:add")
    @Operation(summary = "添加工单处理记录")
    public ResponseEntity<Result<TicketDetailResponse>> addRecord(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody AddTicketRecordRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.addRecord(id, request)));
    }

    @PostMapping("/{id}/resolve")
    @SaCheckPermission("ticket:resolve")
    @Operation(summary = "提交工单解决结果")
    public ResponseEntity<Result<TicketDetailResponse>> resolve(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ResolveTicketRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.resolve(id, request)));
    }

    @PostMapping("/{id}/confirm")
    @SaCheckPermission("ticket:confirm")
    @Operation(summary = "确认工单已解决")
    public ResponseEntity<Result<TicketDetailResponse>> confirm(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ConfirmTicketRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.confirm(id, request)));
    }

    @PostMapping("/{id}/return")
    @SaCheckPermission("ticket:confirm")
    @Operation(summary = "退回工单继续处理")
    public ResponseEntity<Result<TicketDetailResponse>> returnForRework(
            @Positive(message = "工单 ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody ReturnTicketRequest request) {
        return ResponseEntity.ok(Result.success(ticketService.returnForRework(id, request)));
    }
}
