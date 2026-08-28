package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.CreateTicketCategoryRequest;
import com.example.demo.dto.TicketCategoryQuery;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateTicketCategoryRequest;
import com.example.demo.entity.TicketCategory;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TicketCategoryMapper;
import com.example.demo.service.TicketCategoryAdminService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TicketCategoryAdminServiceImpl implements TicketCategoryAdminService {

    private static final int ENABLED = 1;

    private final TicketCategoryMapper categoryMapper;
    private final ReferenceDataConverter converter;

    public TicketCategoryAdminServiceImpl(
            TicketCategoryMapper categoryMapper,
            ReferenceDataConverter converter) {
        this.categoryMapper = categoryMapper;
        this.converter = converter;
    }

    @Override
    public PageResponse<TicketCategoryResponse> page(TicketCategoryQuery query) {
        String keyword = trimToNull(query.keyword());
        Page<TicketCategory> page = categoryMapper.selectPage(
                new Page<>(query.normalizedPageNum(), query.normalizedPageSize()),
                new LambdaQueryWrapper<TicketCategory>()
                        .eq(query.normalizedStatus() != null, TicketCategory::getStatus, query.normalizedStatus())
                        .and(keyword != null, wrapper -> wrapper
                                .like(TicketCategory::getCode, keyword)
                                .or()
                                .like(TicketCategory::getName, keyword))
                        .orderByAsc(TicketCategory::getSortOrder, TicketCategory::getId)
        );
        List<TicketCategoryResponse> records = page.getRecords().stream()
                .map(converter::toCategoryResponse)
                .toList();
        return PageResponse.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public TicketCategoryResponse create(CreateTicketCategoryRequest request) {
        String code = normalizeCode(request.code());
        requireUniqueCode(code, null);

        TicketCategory category = new TicketCategory();
        category.setCode(code);
        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        category.setStatus(ENABLED);
        category.setSortOrder(request.sortOrder());
        category.setCreateBy(currentUserId());
        category.setUpdateBy(currentUserId());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("工单分类编码已存在");
        }
        return converter.toCategoryResponse(requireCategory(category.getId()));
    }

    @Override
    @Transactional
    public TicketCategoryResponse update(Long id, UpdateTicketCategoryRequest request) {
        requireCategory(id);
        String code = normalizeCode(request.code());
        requireUniqueCode(code, id);

        try {
            categoryMapper.update(null, new LambdaUpdateWrapper<TicketCategory>()
                    .eq(TicketCategory::getId, id)
                    .set(TicketCategory::getCode, code)
                    .set(TicketCategory::getName, request.name().trim())
                    .set(TicketCategory::getDescription, trimToNull(request.description()))
                    .set(TicketCategory::getSortOrder, request.sortOrder())
                    .set(TicketCategory::getUpdateBy, currentUserId()));
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("工单分类编码已存在");
        }
        return converter.toCategoryResponse(requireCategory(id));
    }

    @Override
    @Transactional
    public TicketCategoryResponse updateStatus(Long id, UpdateStatusRequest request) {
        TicketCategory category = requireCategory(id);
        category.setStatus(request.status());
        category.setUpdateBy(currentUserId());
        categoryMapper.updateById(category);
        return converter.toCategoryResponse(requireCategory(id));
    }

    private void requireUniqueCode(String code, Long excludedId) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<TicketCategory>()
                .eq(TicketCategory::getCode, code)
                .ne(excludedId != null, TicketCategory::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessConflictException("工单分类编码已存在");
        }
    }

    private TicketCategory requireCategory(Long id) {
        TicketCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ResourceNotFoundException("工单分类不存在");
        }
        return category;
    }

    private Long currentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
