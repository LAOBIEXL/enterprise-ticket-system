package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.TicketCategory;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.service.impl.ReferenceDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceDataServiceImplTests {

    private SysDepartmentMapper departmentMapper;
    private TicketCategoryMapper categoryMapper;
    private ReferenceDataServiceImpl service;

    @BeforeEach
    void setUp() {
        departmentMapper = mock(SysDepartmentMapper.class);
        categoryMapper = mock(TicketCategoryMapper.class);
        service = new ReferenceDataServiceImpl(
                departmentMapper, categoryMapper, new ReferenceDataConverter()
        );
    }

    @Test
    void shouldReturnDepartmentReferenceItems() {
        SysDepartment department = new SysDepartment();
        department.setId(2L);
        department.setCode("TECHNOLOGY");
        department.setName("技术部");
        when(departmentMapper.selectList(any(Wrapper.class))).thenReturn(List.of(department));

        List<ReferenceItemResponse> result = service.getEnabledDepartments();

        assertThat(result).containsExactly(new ReferenceItemResponse("2", "TECHNOLOGY", "技术部"));
    }

    @Test
    void shouldReturnCategoryReferenceItems() {
        TicketCategory category = new TicketCategory();
        category.setId(3L);
        category.setCode("SOFTWARE");
        category.setName("软件安装");
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(category));

        List<ReferenceItemResponse> result = service.getEnabledTicketCategories();

        assertThat(result).containsExactly(new ReferenceItemResponse("3", "SOFTWARE", "软件安装"));
    }
}
