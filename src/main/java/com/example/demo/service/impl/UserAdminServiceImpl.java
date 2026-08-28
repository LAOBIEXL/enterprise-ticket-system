package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.PageResponse;
import com.example.demo.converter.UserAdminConverter;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.ReplaceUserRolesRequest;
import com.example.demo.dto.ResetUserPasswordRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserAdminQuery;
import com.example.demo.dto.UserAdminResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysRole;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.SysDepartmentMapper;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.UserAdminService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysUserMapper userMapper;
    private final SysDepartmentMapper departmentMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAdminConverter converter;

    public UserAdminServiceImpl(
            SysUserMapper userMapper,
            SysDepartmentMapper departmentMapper,
            SysRoleMapper roleMapper,
            PasswordEncoder passwordEncoder,
            UserAdminConverter converter) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.converter = converter;
    }

    @Override
    public PageResponse<UserAdminResponse> page(UserAdminQuery query) {
        String keyword = trimToNull(query.keyword());
        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.normalizedPageNum(), query.normalizedPageSize()),
                new LambdaQueryWrapper<SysUser>()
                        .eq(query.departmentId() != null, SysUser::getDepartmentId, query.departmentId())
                        .eq(query.normalizedStatus() != null, SysUser::getStatus, query.normalizedStatus())
                        .and(keyword != null, wrapper -> wrapper
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getName, keyword)
                                .or().like(SysUser::getEmail, keyword))
                        .orderByDesc(SysUser::getId)
        );
        List<UserAdminResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserAdminResponse getById(Long id) {
        return toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public UserAdminResponse create(CreateUserRequest request) {
        SysDepartment department = requireEnabledDepartment(request.departmentId());
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        requireUniqueUsername(username);
        requireUniqueEmail(email, null);
        Long operatorId = currentUserId();

        SysUser user = new SysUser();
        user.setDepartmentId(department.getId());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setMobile(trimToNull(request.mobile()));
        user.setStatus(ENABLED);
        user.setCreateBy(operatorId);
        user.setUpdateBy(operatorId);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("用户名或邮箱已存在");
        }
        return toResponse(requireUser(user.getId()));
    }

    @Override
    @Transactional
    public UserAdminResponse update(Long id, UpdateUserRequest request) {
        requireUser(id);
        requireEnabledDepartment(request.departmentId());
        String email = normalizeEmail(request.email());
        requireUniqueEmail(email, id);

        try {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, id)
                    .set(SysUser::getDepartmentId, request.departmentId())
                    .set(SysUser::getName, request.name().trim())
                    .set(SysUser::getEmail, email)
                    .set(SysUser::getMobile, trimToNull(request.mobile()))
                    .set(SysUser::getUpdateBy, currentUserId()));
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("邮箱已存在");
        }
        return toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public UserAdminResponse updateStatus(Long id, UpdateStatusRequest request) {
        SysUser user = requireUser(id);
        Long operatorId = currentUserId();
        if (id.equals(operatorId) && request.status() == DISABLED) {
            throw new BusinessConflictException("不能停用当前登录账号");
        }
        user.setStatus(request.status());
        user.setUpdateBy(operatorId);
        userMapper.updateById(user);
        if (request.status() == DISABLED) {
            StpUtil.kickout(id);
        }
        return toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public UserAdminResponse replaceRoles(Long id, ReplaceUserRolesRequest request) {
        requireUser(id);
        Long operatorId = currentUserId();
        if (id.equals(operatorId)) {
            throw new BusinessConflictException("不能修改当前登录账号的角色");
        }
        Set<Long> roleIds = request.roleIds();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            boolean allEnabled = roles.size() == roleIds.size()
                    && roles.stream().allMatch(role -> role.getStatus() == ENABLED);
            if (!allEnabled) {
                throw new BusinessConflictException("角色不存在或已停用");
            }
        }
        userMapper.deleteUserRoles(id);
        roleIds.forEach(roleId -> userMapper.insertUserRoleWithCreator(id, roleId, operatorId));
        StpUtil.kickout(id);
        return toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetUserPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdateBy(currentUserId());
        userMapper.updateById(user);
        StpUtil.kickout(id);
    }

    private UserAdminResponse toResponse(SysUser user) {
        return converter.toResponse(
                user,
                departmentMapper.selectById(user.getDepartmentId()),
                userMapper.selectRolesByUserId(user.getId())
        );
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    private SysDepartment requireEnabledDepartment(Long id) {
        SysDepartment department = departmentMapper.selectById(id);
        if (department == null || department.getStatus() != ENABLED) {
            throw new BusinessConflictException("部门不存在或已停用");
        }
        return department;
    }

    private void requireUniqueUsername(String username) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessConflictException("用户名已存在");
        }
    }

    private void requireUniqueEmail(String email, Long excludedId) {
        if (email == null) {
            return;
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .ne(excludedId != null, SysUser::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessConflictException("邮箱已存在");
        }
    }

    private Long currentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String normalizeEmail(String email) {
        String value = trimToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
