package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.CreateTicketCategoryRequest;
import com.example.demo.dto.TicketCategoryQuery;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.entity.TicketCategory;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.service.impl.TicketCategoryAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketCategoryAdminServiceImplTests {

    private TicketCategoryMapper categoryMapper;
    private TicketCategoryAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(TicketCategoryMapper.class);
        service = new TicketCategoryAdminServiceImpl(categoryMapper, new ReferenceDataConverter());
    }

    @Test
    void shouldCreateEnabledCategoryWithNormalizedCodeAndAuditUser() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            TicketCategory category = invocation.getArgument(0);
            category.setId(9L);
            return 1;
        }).when(categoryMapper).insert(any(TicketCategory.class));
        when(categoryMapper.selectById(9L)).thenAnswer(invocation -> category(9L, "HARDWARE", 1));

        TicketCategoryResponse response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            response = service.create(new CreateTicketCategoryRequest(
                    " hardware ", " 硬件故障 ", " 外设问题 ", 10
            ));
        }

        assertThat(response.id()).isEqualTo("9");
        assertThat(response.code()).isEqualTo("HARDWARE");
        verify(categoryMapper).insert(org.mockito.ArgumentMatchers.<TicketCategory>argThat(category ->
                "HARDWARE".equals(category.getCode())
                        && "硬件故障".equals(category.getName())
                        && Long.valueOf(7L).equals(category.getCreateBy())
                        && Long.valueOf(7L).equals(category.getUpdateBy())
                        && Integer.valueOf(1).equals(category.getStatus())
        ));
    }

    @Test
    void shouldRejectDuplicateCategoryCode() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new CreateTicketCategoryRequest(
                "SOFTWARE", "软件", null, 10
        ))).isInstanceOf(BusinessConflictException.class)
                .hasMessage("工单分类编码已存在");

        verify(categoryMapper, never()).insert(any(TicketCategory.class));
    }

    @Test
    void shouldRejectStatusUpdateForMissingCategory() {
        when(categoryMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStatus(99L, new UpdateStatusRequest(0)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("工单分类不存在");
    }

    @Test
    void shouldMapInternalMyBatisPageToApiPageResponse() {
        doAnswer(invocation -> {
            Page<TicketCategory> requested = invocation.getArgument(0);
            requested.setRecords(List.of(category(1L, "SOFTWARE", 1)));
            requested.setTotal(1);
            return requested;
        }).when(categoryMapper).selectPage(any(IPage.class), any(Wrapper.class));

        PageResponse<TicketCategoryResponse> response = service.page(
                new TicketCategoryQuery(null, "1", 1, 10)
        );

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).extracting(TicketCategoryResponse::code)
                .containsExactly("SOFTWARE");
    }

    private TicketCategory category(Long id, String code, Integer status) {
        TicketCategory category = new TicketCategory();
        category.setId(id);
        category.setCode(code);
        category.setName("分类名称");
        category.setStatus(status);
        category.setSortOrder(10);
        return category;
    }
}
