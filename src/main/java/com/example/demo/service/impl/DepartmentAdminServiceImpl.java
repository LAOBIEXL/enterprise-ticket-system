package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.ReferenceDataConverter;
import com.example.demo.dto.CreateDepartmentRequest;
import com.example.demo.dto.DepartmentQuery;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.dto.UpdateDepartmentRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.DepartmentAdminService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class DepartmentAdminServiceImpl implements DepartmentAdminService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;
    private final ReferenceDataConverter converter;

    public DepartmentAdminServiceImpl(
            SysDepartmentMapper departmentMapper,
            SysUserMapper userMapper,
            ReferenceDataConverter converter) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
        this.converter = converter;
    }

    @Override
    public PageResponse<DepartmentResponse> page(DepartmentQuery query) {
        String keyword = trimToNull(query.keyword());
        Page<SysDepartment> page = departmentMapper.selectPage(
                new Page<>(query.normalizedPageNum(), query.normalizedPageSize()),
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(query.normalizedStatus() != null, SysDepartment::getStatus, query.normalizedStatus())
                        .and(keyword != null, wrapper -> wrapper
                                .like(SysDepartment::getCode, keyword)
                                .or()
                                .like(SysDepartment::getName, keyword))
                        .orderByAsc(SysDepartment::getSortOrder, SysDepartment::getId)
        );
        List<DepartmentResponse> records = page.getRecords().stream()
                .map(converter::toDepartmentResponse)
                .toList();
        return PageResponse.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String code = normalizeCode(request.code());
        requireUniqueCode(code, null);
        Long operatorId = currentUserId();

        SysDepartment department = new SysDepartment();
        department.setCode(code);
        department.setName(request.name().trim());
        department.setStatus(ENABLED);
        department.setSortOrder(request.sortOrder());
        department.setCreateBy(operatorId);
        department.setUpdateBy(operatorId);
        try {
            departmentMapper.insert(department);
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("部门编码已存在");
        }
        return converter.toDepartmentResponse(requireDepartment(department.getId()));
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        requireDepartment(id);
        String code = normalizeCode(request.code());
        requireUniqueCode(code, id);

        try {
            departmentMapper.update(null, new LambdaUpdateWrapper<SysDepartment>()
                    .eq(SysDepartment::getId, id)
                    .set(SysDepartment::getCode, code)
                    .set(SysDepartment::getName, request.name().trim())
                    .set(SysDepartment::getSortOrder, request.sortOrder())
                    .set(SysDepartment::getUpdateBy, currentUserId()));
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("部门编码已存在");
        }
        return converter.toDepartmentResponse(requireDepartment(id));
    }

    @Override
    @Transactional
    public DepartmentResponse updateStatus(Long id, UpdateStatusRequest request) {
        SysDepartment department = requireDepartment(id);
        if (request.status() == DISABLED && department.getStatus() != DISABLED) {
            Long enabledUsers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getDepartmentId, id)
                    .eq(SysUser::getStatus, ENABLED));
            if (enabledUsers != null && enabledUsers > 0) {
                throw new BusinessConflictException("部门下仍有启用用户，不能停用");
            }
        }
        department.setStatus(request.status());
        department.setUpdateBy(currentUserId());
        departmentMapper.updateById(department);
        return converter.toDepartmentResponse(requireDepartment(id));
    }

    private void requireUniqueCode(String code, Long excludedId) {
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<SysDepartment>()
                .eq(SysDepartment::getCode, code)
                .ne(excludedId != null, SysDepartment::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessConflictException("部门编码已存在");
        }
    }

    private SysDepartment requireDepartment(Long id) {
        SysDepartment department = departmentMapper.selectById(id);
        if (department == null) {
            throw new ResourceNotFoundException("部门不存在");
        }
        return department;
    }

    private Long currentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
