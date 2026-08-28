package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.TicketConverter;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketQuery;
import com.example.demo.dto.TicketSummaryResponse;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketCategory;
import com.example.demo.entity.TicketRecord;
import com.example.demo.exception.DataScopeForbiddenException;
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
