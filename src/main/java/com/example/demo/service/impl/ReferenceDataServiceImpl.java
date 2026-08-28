package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.TicketCategory;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.service.ReferenceDataService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReferenceDataServiceImpl implements ReferenceDataService {

    private static final int ENABLED = 1;

    private final SysDepartmentMapper departmentMapper;
    private final TicketCategoryMapper categoryMapper;
    private final ReferenceDataConverter converter;

    public ReferenceDataServiceImpl(
            SysDepartmentMapper departmentMapper,
            TicketCategoryMapper categoryMapper,
            ReferenceDataConverter converter) {
        this.departmentMapper = departmentMapper;
        this.categoryMapper = categoryMapper;
        this.converter = converter;
    }

    @Override
    public List<ReferenceItemResponse> getEnabledDepartments() {
        return departmentMapper.selectList(new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getStatus, ENABLED)
                        .orderByAsc(SysDepartment::getSortOrder, SysDepartment::getId))
                .stream()
                .map(converter::toReference)
                .toList();
    }

    @Override
    public List<ReferenceItemResponse> getEnabledTicketCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<TicketCategory>()
                        .eq(TicketCategory::getStatus, ENABLED)
                        .orderByAsc(TicketCategory::getSortOrder, TicketCategory::getId))
                .stream()
                .map(converter::toReference)
                .toList();
    }
}
