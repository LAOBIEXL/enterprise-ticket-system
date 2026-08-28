package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.TicketConverter;
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
import com.example.demo.entity.SysUser;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketAction;
import com.example.demo.entity.TicketCategory;
import com.example.demo.entity.TicketRecord;
import com.example.demo.entity.TicketStatus;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.DataScopeForbiddenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.mapper.TicketRecordMapper;
import com.example.demo.mapper.model.TicketQueryCriteria;
import com.example.demo.mapper.model.TicketViewRow;
import com.example.demo.service.TicketService;
import com.example.demo.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
    private static final int ENABLED = 1;
    private static final String IDEMPOTENCY_PREFIX = "ticket:create:";
    private static final String PROCESSING = "PROCESSING";
    private static final String TICKET_REFERENCE_PREFIX = "TICKET:";

    private final TicketMapper ticketMapper;
    private final TicketRecordMapper ticketRecordMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketConverter ticketConverter;
    private final RedisUtils redisUtils;

    public TicketServiceImpl(
            TicketMapper ticketMapper,
            TicketRecordMapper ticketRecordMapper,
            TicketCategoryMapper ticketCategoryMapper,
            SysUserMapper sysUserMapper,
            TicketConverter ticketConverter,
            RedisUtils redisUtils) {
        this.ticketMapper = ticketMapper;
        this.ticketRecordMapper = ticketRecordMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.ticketConverter = ticketConverter;
        this.redisUtils = redisUtils;
    }

    @Override
    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request, String idempotencyKey) {
        Long userId = currentUserId();
        String redisKey = IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey;

        TicketDetailResponse previous = findIdempotentResult(redisKey, userId);
        if (previous != null) {
            return previous;
        }
        if (!redisUtils.setIfAbsent(redisKey, PROCESSING, 5, TimeUnit.MINUTES)) {
            previous = findIdempotentResult(redisKey, userId);
            if (previous != null) {
                return previous;
            }
            throw new BusinessConflictException("相同的工单创建请求正在处理中，请稍后重试");
        }

        try {
            SysUser requester = requireEnabledUser(userId);
            requireEnabledCategory(request.categoryId());

            Ticket ticket = new Ticket();
            ticket.setTicketNo(generateTicketNo());
            ticket.setTitle(request.title().trim());
            ticket.setDescription(request.description().trim());
            ticket.setCategoryId(request.categoryId());
            ticket.setRequesterId(userId);
            ticket.setRequesterDepartmentId(requester.getDepartmentId());
            ticket.setStatus(TicketStatus.PENDING.name());
            ticket.setVersion(0);
            ticketMapper.insert(ticket);

            TicketRecord record = new TicketRecord();
            record.setTicketId(ticket.getId());
            record.setOperatorId(userId);
            record.setAction(TicketAction.CREATE.name());
            record.setToStatus(TicketStatus.PENDING.name());
            record.setContent("创建工单");
            ticketRecordMapper.insert(record);

            TicketDetailResponse response = loadDetail(ticket.getId());
            completeIdempotencyAfterCommit(redisKey, ticket.getId());
            return response;
        } catch (RuntimeException e) {
            safeDeleteIdempotencyKey(redisKey);
            throw e;
        }
    }

    @Override
    public PageResponse<TicketSummaryResponse> getMine(TicketQuery query) {
        return query(query, currentUserId(), null);
    }

    @Override
    public PageResponse<TicketSummaryResponse> getAssigned(TicketQuery query) {
        return query(query, null, currentUserId());
    }

    @Override
    public PageResponse<TicketSummaryResponse> getAll(TicketQuery query) {
        return query(query, query.requesterId(), query.assigneeId());
    }

    @Override
    public TicketDetailResponse getDetail(Long id) {
        TicketViewRow row = requireTicket(id);
        Long userId = currentUserId();
        boolean canRead = StpUtil.hasPermission("ticket:read:all")
                || (userId.equals(row.getRequesterId()) && StpUtil.hasPermission("ticket:read:own"))
                || (userId.equals(row.getAssigneeId()) && StpUtil.hasPermission("ticket:read:assigned"));
        if (!canRead) {
            throw new DataScopeForbiddenException("没有查看该工单的数据权限");
        }
        return ticketConverter.toDetail(row, ticketMapper.selectRecordsByTicketId(id));
    }

    @Override
    @Transactional
    public TicketDetailResponse assign(Long id, AssignTicketRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.PENDING);
        requireTechnicalAssignee(request.assigneeId());

        updateAssignment(current, TicketStatus.ASSIGNED, request.assigneeId());
        appendRecord(
                current, operatorId, request.assigneeId(), TicketAction.ASSIGN,
                TicketStatus.ASSIGNED, defaultText(request.remark(), "分派工单")
        );
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse reassign(Long id, ReassignTicketRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.ASSIGNED, TicketStatus.PROCESSING);
        if (request.assigneeId().equals(current.getAssigneeId())) {
            throw new BusinessConflictException("改派目标不能与当前处理人相同");
        }
        requireTechnicalAssignee(request.assigneeId());

        updateAssignment(current, TicketStatus.ASSIGNED, request.assigneeId());
        appendRecord(
                current, operatorId, request.assigneeId(), TicketAction.REASSIGN,
                TicketStatus.ASSIGNED, request.reason().trim()
        );
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse start(Long id) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.ASSIGNED);
        requireCurrentAssignee(current, operatorId);

        updateStatus(current, TicketStatus.PROCESSING, null, null);
        appendRecord(current, operatorId, null, TicketAction.START, TicketStatus.PROCESSING, "开始处理工单");
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse addRecord(Long id, AddTicketRecordRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.PROCESSING);
        requireCurrentAssignee(current, operatorId);

        updateStatus(current, TicketStatus.PROCESSING, null, null);
        appendRecord(
                current, operatorId, null, TicketAction.ADD_RECORD,
                TicketStatus.PROCESSING, request.content().trim()
        );
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse resolve(Long id, ResolveTicketRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.PROCESSING);
        requireCurrentAssignee(current, operatorId);

        updateStatus(current, TicketStatus.WAIT_CONFIRM, LocalDateTime.now(ZoneOffset.UTC), null);
        appendRecord(
                current, operatorId, null, TicketAction.RESOLVE,
                TicketStatus.WAIT_CONFIRM, request.solution().trim()
        );
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse confirm(Long id, ConfirmTicketRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.WAIT_CONFIRM);
        requireRequester(current, operatorId);

        updateStatus(current, TicketStatus.CLOSED, null, LocalDateTime.now(ZoneOffset.UTC));
        appendRecord(
                current, operatorId, null, TicketAction.CONFIRM,
                TicketStatus.CLOSED, defaultText(request.remark(), "确认工单已解决")
        );
        return loadDetail(id);
    }

    @Override
    @Transactional
    public TicketDetailResponse returnForRework(Long id, ReturnTicketRequest request) {
        Long operatorId = currentUserId();
        TicketViewRow current = requireTicket(id);
        requireState(current, TicketStatus.WAIT_CONFIRM);
        requireRequester(current, operatorId);

        updateStatus(current, TicketStatus.PROCESSING, null, null);
        appendRecord(
                current, operatorId, null, TicketAction.RETURN,
                TicketStatus.PROCESSING, request.reason().trim()
        );
        return loadDetail(id);
    }

    private PageResponse<TicketSummaryResponse> query(TicketQuery query, Long requesterId, Long assigneeId) {
        TicketQueryCriteria criteria = new TicketQueryCriteria(
                query.status(), query.categoryId(), requesterId, assigneeId, normalizeKeyword(query.keyword())
        );
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        long total = ticketMapper.countByCriteria(criteria);
        List<TicketSummaryResponse> records = total == 0
                ? List.of()
                : ticketMapper.selectPageByCriteria(criteria, (long) (pageNum - 1) * pageSize, pageSize)
                .stream().map(ticketConverter::toSummary).toList();
        return PageResponse.of(records, total, pageNum, pageSize);
    }

    private void updateAssignment(TicketViewRow current, TicketStatus toStatus, Long assigneeId) {
        int updated = ticketMapper.updateAssignment(
                current.getId(), current.getStatus(), current.getVersion(), toStatus.name(), assigneeId
        );
        requireUpdated(updated);
    }

    private void updateStatus(
            TicketViewRow current,
            TicketStatus toStatus,
            LocalDateTime resolvedTime,
            LocalDateTime closedTime) {
        int updated = ticketMapper.updateStatus(
                current.getId(), current.getStatus(), current.getVersion(),
                toStatus.name(), resolvedTime, closedTime
        );
        requireUpdated(updated);
    }

    private void requireUpdated(int updated) {
        if (updated != 1) {
            throw new BusinessConflictException("工单状态已变化，请刷新后重试");
        }
    }

    private void appendRecord(
            TicketViewRow current,
            Long operatorId,
            Long targetUserId,
            TicketAction action,
            TicketStatus toStatus,
            String content) {
        TicketRecord record = new TicketRecord();
        record.setTicketId(current.getId());
        record.setOperatorId(operatorId);
        record.setTargetUserId(targetUserId);
        record.setAction(action.name());
        record.setFromStatus(current.getStatus());
        record.setToStatus(toStatus.name());
        record.setContent(content);
        ticketRecordMapper.insert(record);
    }

    private void requireState(TicketViewRow current, TicketStatus... allowedStatuses) {
        for (TicketStatus allowedStatus : allowedStatuses) {
            if (allowedStatus.name().equals(current.getStatus())) {
                return;
            }
        }
        throw new BusinessConflictException("当前工单状态不允许执行该操作");
    }

    private void requireCurrentAssignee(TicketViewRow current, Long operatorId) {
        if (!operatorId.equals(current.getAssigneeId())) {
            throw new DataScopeForbiddenException("只有当前处理人可以执行该操作");
        }
    }

    private void requireRequester(TicketViewRow current, Long operatorId) {
        if (!operatorId.equals(current.getRequesterId())) {
            throw new DataScopeForbiddenException("只有工单提交人可以执行该操作");
        }
    }

    private void requireTechnicalAssignee(Long assigneeId) {
        SysUser assignee = sysUserMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new ResourceNotFoundException("目标处理人不存在");
        }
        List<String> roles = sysUserMapper.selectEnabledRoleCodesByUserId(assigneeId);
        boolean technicalRole = roles.contains("TECHNICIAN") || roles.contains("SUPPORT_MANAGER");
        if (!Integer.valueOf(ENABLED).equals(assignee.getStatus()) || !technicalRole) {
            throw new BusinessConflictException("目标处理人已停用或不具备技术处理角色");
        }
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private TicketDetailResponse findIdempotentResult(String redisKey, Long requesterId) {
        String value = redisUtils.get(redisKey, String.class);
        if (value == null || !value.startsWith(TICKET_REFERENCE_PREFIX)) {
            return null;
        }
        Long ticketId = Long.valueOf(value.substring(TICKET_REFERENCE_PREFIX.length()));
        TicketViewRow row = requireTicket(ticketId);
        if (!requesterId.equals(row.getRequesterId())) {
            throw new DataScopeForbiddenException("幂等键对应的工单不属于当前用户");
        }
        return ticketConverter.toDetail(row, ticketMapper.selectRecordsByTicketId(ticketId));
    }

    private void completeIdempotencyAfterCommit(String redisKey, Long ticketId) {
        Runnable saveReference = () -> redisUtils.set(
                redisKey, TICKET_REFERENCE_PREFIX + ticketId, 24, TimeUnit.HOURS
        );
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            saveReference.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    saveReference.run();
                } catch (RuntimeException e) {
                    log.error("工单已提交，但保存创建幂等结果失败，ticketId={}", ticketId, e);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    safeDeleteIdempotencyKey(redisKey);
                }
            }
        });
    }

    private void safeDeleteIdempotencyKey(String redisKey) {
        try {
            redisUtils.delete(redisKey);
        } catch (RuntimeException cleanupError) {
            log.warn("清理工单创建幂等标记失败，key={}", redisKey, cleanupError);
        }
    }

    private SysUser requireEnabledUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Integer.valueOf(ENABLED).equals(user.getStatus())) {
            throw new ResourceNotFoundException("当前用户不存在或已停用");
        }
        if (user.getDepartmentId() == null) {
            throw new BusinessConflictException("当前用户尚未配置所属部门");
        }
        return user;
    }

    private void requireEnabledCategory(Long categoryId) {
        TicketCategory category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(ENABLED).equals(category.getStatus())) {
            throw new ResourceNotFoundException("工单分类不存在或已停用");
        }
    }

    private TicketDetailResponse loadDetail(Long id) {
        TicketViewRow row = requireTicket(id);
        return ticketConverter.toDetail(row, ticketMapper.selectRecordsByTicketId(id));
    }

    private TicketViewRow requireTicket(Long id) {
        TicketViewRow row = ticketMapper.selectDetailById(id);
        if (row == null) {
            throw new ResourceNotFoundException("工单不存在");
        }
        return row;
    }

    private Long currentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String generateTicketNo() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "TK" + date + random;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
