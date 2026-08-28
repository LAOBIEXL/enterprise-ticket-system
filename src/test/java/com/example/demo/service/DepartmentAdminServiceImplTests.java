package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.CreateDepartmentRequest;
import com.example.demo.dto.DepartmentQuery;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.impl.DepartmentAdminServiceImpl;
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

class DepartmentAdminServiceImplTests {

    private SysDepartmentMapper departmentMapper;
    private SysUserMapper userMapper;
    private DepartmentAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        departmentMapper = mock(SysDepartmentMapper.class);
        userMapper = mock(SysUserMapper.class);
        service = new DepartmentAdminServiceImpl(
                departmentMapper, userMapper, new ReferenceDataConverter()
        );
    }

    @Test
    void shouldCreateEnabledDepartmentWithNormalizedCode() {
        when(departmentMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            SysDepartment department = invocation.getArgument(0);
            department.setId(8L);
            return 1;
        }).when(departmentMapper).insert(any(SysDepartment.class));
        when(departmentMapper.selectById(8L)).thenReturn(department(8L, "CUSTOMER_SERVICE", 1));

        DepartmentResponse response;
        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginId).thenReturn(7L);
            response = service.create(new CreateDepartmentRequest(
                    " customer_service ", " 客服部 ", 60
            ));
        }

        assertThat(response.id()).isEqualTo("8");
        verify(departmentMapper).insert(org.mockito.ArgumentMatchers.<SysDepartment>argThat(department ->
                "CUSTOMER_SERVICE".equals(department.getCode())
                        && "客服部".equals(department.getName())
                        && Long.valueOf(7L).equals(department.getCreateBy())
                        && Integer.valueOf(1).equals(department.getStatus())
        ));
    }

    @Test
    void shouldRejectDuplicateDepartmentCode() {
        when(departmentMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new CreateDepartmentRequest(
                "TECHNOLOGY", "技术部", 10
        ))).isInstanceOf(BusinessConflictException.class)
                .hasMessage("部门编码已存在");

        verify(departmentMapper, never()).insert(any(SysDepartment.class));
    }

    @Test
    void shouldRejectDisablingDepartmentWithEnabledUsers() {
        when(departmentMapper.selectById(5L)).thenReturn(department(5L, "TECHNOLOGY", 1));
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        assertThatThrownBy(() -> service.updateStatus(5L, new UpdateStatusRequest(0)))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("部门下仍有启用用户，不能停用");

        verify(departmentMapper, never()).updateById(any(SysDepartment.class));
    }

    @Test
    void shouldRejectStatusUpdateForMissingDepartment() {
        when(departmentMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateStatus(99L, new UpdateStatusRequest(0)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("部门不存在");
    }

    @Test
    void shouldMapInternalPageToApiPageResponse() {
        doAnswer(invocation -> {
            Page<SysDepartment> requested = invocation.getArgument(0);
            requested.setRecords(List.of(department(5L, "TECHNOLOGY", 1)));
            requested.setTotal(1);
            return requested;
        }).when(departmentMapper).selectPage(any(IPage.class), any(Wrapper.class));

        PageResponse<DepartmentResponse> response = service.page(
                new DepartmentQuery("技术", "1", 1, 10)
        );

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).extracting(DepartmentResponse::code)
                .containsExactly("TECHNOLOGY");
    }

    private SysDepartment department(Long id, String code, Integer status) {
        SysDepartment department = new SysDepartment();
        department.setId(id);
        department.setCode(code);
        department.setName("部门名称");
        department.setStatus(status);
        department.setSortOrder(10);
        return department;
    }
}
