package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.AssignTicketRequest;
import com.example.demo.dto.AddTicketRecordRequest;
import com.example.demo.dto.ConfirmTicketRequest;
import com.example.demo.dto.ReassignTicketRequest;
import com.example.demo.dto.ReturnTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketQuery;
import com.example.demo.dto.TicketSummaryResponse;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketCategory;
import com.example.demo.entity.TicketRecord;
import com.example.demo.exception.DataScopeForbiddenException;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.mapper.TicketRecordMapper;
import com.example.demo.mapper.model.TicketQueryCriteria;
import com.example.demo.mapper.model.TicketViewRow;
import com.example.demo.service.impl.TicketServiceImpl;
import com.example.demo.util.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceImplTests {

    private TicketMapper ticketMapper;
    private TicketRecordMapper recordMapper;
    private TicketCategoryMapper categoryMapper;
    private SysUserMapper userMapper;
    private RedisUtils redisUtils;
    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        ticketMapper = mock(TicketMapper.class);
        recordMapper = mock(TicketRecordMapper.class);
        categoryMapper = mock(TicketCategoryMapper.class);
        userMapper = mock(SysUserMapper.class);
        redisUtils = mock(RedisUtils.class);
        service = new TicketServiceImpl(
                ticketMapper, recordMapper, categoryMapper, userMapper,
                new TicketConverter(), redisUtils
        );
    }

    @Test
    void shouldCreatePendingTicketAndAppendCreateRecord() {
        SysUser requester = enabledUser(7L, 2L);
        TicketCategory category = enabledCategory(3L);
        TicketViewRow createdRow = row(100L, 7L, null);
        String key = "ticket:create:7:550e8400-e29b-41d4-a716-446655440000";
        when(redisUtils.get(key, String.class)).thenReturn(null);
        when(redisUtils.setIfAbsent(key, "PROCESSING", 5, TimeUnit.MINUTES)).thenReturn(true);
        when(userMapper.selectById(7L)).thenReturn(requester);
        when(categoryMapper.selectById(3L)).thenReturn(category);
        doAnswer(invocation -> {
            invocation.<Ticket>getArgument(0).setId(100L);
            return 1;
        }).when(ticketMapper).insert(any(Ticket.class));
        when(ticketMapper.selectDetailById(100L)).thenReturn(createdRow);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            TicketDetailResponse response = service.create(
                    new CreateTicketRequest(3L, "  无法登录  ", "  页面一直加载  "),
                    "550e8400-e29b-41d4-a716-446655440000"
            );
            assertThat(response.id()).isEqualTo("100");
        }

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketMapper).insert(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(ticketCaptor.getValue().getRequesterDepartmentId()).isEqualTo(2L);
        assertThat(ticketCaptor.getValue().getTitle()).isEqualTo("无法登录");

        ArgumentCaptor<TicketRecord> recordCaptor = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getAction()).isEqualTo("CREATE");
        assertThat(recordCaptor.getValue().getToStatus()).isEqualTo("PENDING");
        verify(redisUtils).set(key, "TICKET:100", 24, TimeUnit.HOURS);
    }

    @Test
    void shouldReturnPreviousTicketForRepeatedIdempotencyKey() {
        String key = "ticket:create:7:same-key";
        when(redisUtils.get(key, String.class)).thenReturn("TICKET:100");
        when(ticketMapper.selectDetailById(100L)).thenReturn(row(100L, 7L, null));
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            TicketDetailResponse response = service.create(
                    new CreateTicketRequest(3L, "标题", "描述"), "same-key"
            );
            assertThat(response.id()).isEqualTo("100");
        }

        verify(ticketMapper, never()).insert(any(Ticket.class));
        verify(recordMapper, never()).insert(any(TicketRecord.class));
    }

    @Test
    void shouldForceCurrentUserIntoMineQuery() {
        when(ticketMapper.countByCriteria(any())).thenReturn(0L);

        PageResponse<TicketSummaryResponse> response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            response = service.getMine(new TicketQuery(null, null, 999L, 888L, " test ", null, null));
        }

        ArgumentCaptor<TicketQueryCriteria> criteria = ArgumentCaptor.forClass(TicketQueryCriteria.class);
        verify(ticketMapper).countByCriteria(criteria.capture());
        assertThat(criteria.getValue().requesterId()).isEqualTo(7L);
        assertThat(criteria.getValue().assigneeId()).isNull();
        assertThat(criteria.getValue().keyword()).isEqualTo("test");
        assertThat(response.pageNum()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(10);
    }

    @Test
    void shouldRejectDetailOutsideCurrentUsersDataScope() {
        when(ticketMapper.selectDetailById(100L)).thenReturn(row(100L, 8L, 9L));

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            stp.when(() -> StpUtil.hasPermission("ticket:read:all")).thenReturn(false);

            assertThatThrownBy(() -> service.getDetail(100L))
                    .isInstanceOf(DataScopeForbiddenException.class)
                    .hasMessage("没有查看该工单的数据权限");
        }
    }

    @Test
    void shouldAssignPendingTicketToEnabledTechnician() {
        TicketViewRow pending = row(100L, 7L, null);
        pending.setVersion(4);
        TicketViewRow assigned = row(100L, 7L, 9L);
        assigned.setStatus("ASSIGNED");
        assigned.setVersion(5);
        SysUser technician = enabledUser(9L, 2L);
        when(ticketMapper.selectDetailById(100L)).thenReturn(pending, assigned);
        when(userMapper.selectById(9L)).thenReturn(technician);
        when(userMapper.selectEnabledRoleCodesByUserId(9L)).thenReturn(List.of("TECHNICIAN"));
        when(ticketMapper.updateAssignment(100L, "PENDING", 4, "ASSIGNED", 9L)).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        TicketDetailResponse response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(20L);
            response = service.assign(100L, new AssignTicketRequest(9L, "一线技术处理"));
        }

        assertThat(response.status()).isEqualTo("ASSIGNED");
        ArgumentCaptor<TicketRecord> record = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(record.capture());
        assertThat(record.getValue().getAction()).isEqualTo("ASSIGN");
        assertThat(record.getValue().getFromStatus()).isEqualTo("PENDING");
        assertThat(record.getValue().getToStatus()).isEqualTo("ASSIGNED");
        assertThat(record.getValue().getTargetUserId()).isEqualTo(9L);
    }

    @Test
    void shouldReturnConflictAndNotWriteRecordWhenConditionalUpdateLosesRace() {
        TicketViewRow assigned = row(100L, 7L, 9L);
        assigned.setStatus("ASSIGNED");
        assigned.setVersion(2);
        when(ticketMapper.selectDetailById(100L)).thenReturn(assigned);
        when(ticketMapper.updateStatus(100L, "ASSIGNED", 2, "PROCESSING", null, null))
                .thenReturn(0);

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(9L);
            assertThatThrownBy(() -> service.start(100L))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("工单状态已变化，请刷新后重试");
        }

        verify(recordMapper, never()).insert(any(TicketRecord.class));
    }

    @Test
    void shouldRejectProcessingActionFromNonAssignee() {
        TicketViewRow processing = row(100L, 7L, 9L);
        processing.setStatus("PROCESSING");
        when(ticketMapper.selectDetailById(100L)).thenReturn(processing);

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(10L);
            assertThatThrownBy(() -> service.resolve(
                    100L, new com.example.demo.dto.ResolveTicketRequest("已恢复服务")
            )).isInstanceOf(DataScopeForbiddenException.class)
                    .hasMessage("只有当前处理人可以执行该操作");
        }

        verify(ticketMapper, never()).updateStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldReassignProcessingTicketBackToAssigned() {
        TicketViewRow processing = row(100L, 7L, 9L);
        processing.setStatus("PROCESSING");
        processing.setVersion(6);
        TicketViewRow reassigned = row(100L, 7L, 10L);
        reassigned.setStatus("ASSIGNED");
        reassigned.setVersion(7);
        when(ticketMapper.selectDetailById(100L)).thenReturn(processing, reassigned);
        when(userMapper.selectById(10L)).thenReturn(enabledUser(10L, 2L));
        when(userMapper.selectEnabledRoleCodesByUserId(10L)).thenReturn(List.of("SUPPORT_MANAGER"));
        when(ticketMapper.updateAssignment(100L, "PROCESSING", 6, "ASSIGNED", 10L)).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(20L);
            TicketDetailResponse response = service.reassign(
                    100L, new ReassignTicketRequest(10L, "转交二线支持")
            );
            assertThat(response.status()).isEqualTo("ASSIGNED");
        }

        ArgumentCaptor<TicketRecord> record = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(record.capture());
        assertThat(record.getValue().getAction()).isEqualTo("REASSIGN");
        assertThat(record.getValue().getContent()).isEqualTo("转交二线支持");
    }

    @Test
    void shouldAppendProcessingRecordWithoutChangingState() {
        TicketViewRow processing = row(100L, 7L, 9L);
        processing.setStatus("PROCESSING");
        processing.setVersion(3);
        TicketViewRow updated = row(100L, 7L, 9L);
        updated.setStatus("PROCESSING");
        updated.setVersion(4);
        when(ticketMapper.selectDetailById(100L)).thenReturn(processing, updated);
        when(ticketMapper.updateStatus(100L, "PROCESSING", 3, "PROCESSING", null, null)).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(9L);
            service.addRecord(100L, new AddTicketRecordRequest("已检查服务器日志"));
        }

        ArgumentCaptor<TicketRecord> record = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(record.capture());
        assertThat(record.getValue().getAction()).isEqualTo("ADD_RECORD");
        assertThat(record.getValue().getFromStatus()).isEqualTo("PROCESSING");
        assertThat(record.getValue().getToStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void shouldResolveTicketAndRecordResolvedTime() {
        TicketViewRow processing = row(100L, 7L, 9L);
        processing.setStatus("PROCESSING");
        processing.setVersion(4);
        TicketViewRow waiting = row(100L, 7L, 9L);
        waiting.setStatus("WAIT_CONFIRM");
        waiting.setVersion(5);
        when(ticketMapper.selectDetailById(100L)).thenReturn(processing, waiting);
        when(ticketMapper.updateStatus(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq("PROCESSING"),
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.eq("WAIT_CONFIRM"),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(9L);
            service.resolve(100L, new com.example.demo.dto.ResolveTicketRequest("重启服务后恢复"));
        }

        ArgumentCaptor<TicketRecord> record = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(record.capture());
        assertThat(record.getValue().getAction()).isEqualTo("RESOLVE");
        assertThat(record.getValue().getToStatus()).isEqualTo("WAIT_CONFIRM");
    }

    @Test
    void shouldAllowRequesterToConfirmOrReturnWaitingTicket() {
        TicketViewRow waitingForConfirm = row(100L, 7L, 9L);
        waitingForConfirm.setStatus("WAIT_CONFIRM");
        waitingForConfirm.setVersion(5);
        TicketViewRow closed = row(100L, 7L, 9L);
        closed.setStatus("CLOSED");
        closed.setVersion(6);
        when(ticketMapper.selectDetailById(100L)).thenReturn(waitingForConfirm, closed);
        when(ticketMapper.updateStatus(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq("WAIT_CONFIRM"),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.eq("CLOSED"),
                org.mockito.ArgumentMatchers.isNull(),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(100L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            TicketDetailResponse response = service.confirm(100L, new ConfirmTicketRequest(null));
            assertThat(response.status()).isEqualTo("CLOSED");
        }

        ArgumentCaptor<TicketRecord> record = ArgumentCaptor.forClass(TicketRecord.class);
        verify(recordMapper).insert(record.capture());
        assertThat(record.getValue().getAction()).isEqualTo("CONFIRM");
        assertThat(record.getValue().getContent()).isEqualTo("确认工单已解决");

        TicketViewRow anotherWaiting = row(101L, 7L, 9L);
        anotherWaiting.setStatus("WAIT_CONFIRM");
        anotherWaiting.setVersion(2);
        TicketViewRow returned = row(101L, 7L, 9L);
        returned.setStatus("PROCESSING");
        returned.setVersion(3);
        when(ticketMapper.selectDetailById(101L)).thenReturn(anotherWaiting, returned);
        when(ticketMapper.updateStatus(101L, "WAIT_CONFIRM", 2, "PROCESSING", null, null)).thenReturn(1);
        when(ticketMapper.selectRecordsByTicketId(101L)).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            TicketDetailResponse response = service.returnForRework(
                    101L, new ReturnTicketRequest("问题仍然存在")
            );
            assertThat(response.status()).isEqualTo("PROCESSING");
        }
    }

    private SysUser enabledUser(Long id, Long departmentId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDepartmentId(departmentId);
        user.setStatus(1);
        return user;
    }

    private TicketCategory enabledCategory(Long id) {
        TicketCategory category = new TicketCategory();
        category.setId(id);
        category.setStatus(1);
        return category;
    }

    private TicketViewRow row(Long id, Long requesterId, Long assigneeId) {
        TicketViewRow row = new TicketViewRow();
        row.setId(id);
        row.setTicketNo("TK202608280001");
        row.setTitle("无法登录");
        row.setDescription("页面一直加载");
        row.setStatus("PENDING");
        row.setCategoryId(3L);
        row.setCategoryName("软件问题");
        row.setRequesterId(requesterId);
        row.setRequesterName("张三");
        row.setRequesterDepartmentName("研发部");
        row.setAssigneeId(assigneeId);
        row.setVersion(0);
        return row;
    }
}
