package com.example.demo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.converter.RoleAdminConverter;
import com.example.demo.dto.CreateRoleRequest;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.ReplaceRolePermissionsRequest;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.dto.UpdateRoleRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.entity.SysPermission;
import com.example.demo.entity.SysRole;
import com.example.demo.exception.BusinessConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.SysPermissionMapper;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.service.RolePermissionAdminService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RolePermissionAdminServiceImpl implements RolePermissionAdminService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final RoleAdminConverter converter;

    public RolePermissionAdminServiceImpl(
            SysRoleMapper roleMapper,
            SysPermissionMapper permissionMapper,
            RoleAdminConverter converter) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.converter = converter;
    }

    @Override
    public List<RoleAdminResponse> getRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getCode))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RoleAdminResponse createRole(CreateRoleRequest request) {
        String code = normalizeCode(request.code());
        requireUniqueCode(code, null);
        Long operatorId = currentUserId();
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(trimToNull(request.description()));
        role.setStatus(ENABLED);
        role.setCreateBy(operatorId);
        role.setUpdateBy(operatorId);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("角色编码已存在");
        }
        return toResponse(requireRole(role.getId()));
    }

    @Override
    @Transactional
    public RoleAdminResponse updateRole(Long id, UpdateRoleRequest request) {
        requireRole(id);
        String code = normalizeCode(request.code());
        requireUniqueCode(code, id);
        try {
            roleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                    .eq(SysRole::getId, id)
                    .set(SysRole::getCode, code)
                    .set(SysRole::getName, request.name().trim())
                    .set(SysRole::getDescription, trimToNull(request.description()))
                    .set(SysRole::getUpdateBy, currentUserId()));
        } catch (DuplicateKeyException e) {
            throw new BusinessConflictException("角色编码已存在");
        }
        return toResponse(requireRole(id));
    }

    @Override
    @Transactional
    public RoleAdminResponse updateRoleStatus(Long id, UpdateStatusRequest request) {
        SysRole role = requireRole(id);
        if (request.status() == DISABLED) {
            requireRoleNotAssignedToCurrentUser(id);
            if (roleMapper.countEnabledUsersByRoleId(id) > 0) {
                throw new BusinessConflictException("角色仍分配给启用用户，不能停用");
            }
        }
        role.setStatus(request.status());
        role.setUpdateBy(currentUserId());
        roleMapper.updateById(role);
        invalidateRoleSessions(id);
        return toResponse(requireRole(id));
    }

    @Override
    @Transactional
    public RoleAdminResponse replacePermissions(Long id, ReplaceRolePermissionsRequest request) {
        requireRole(id);
        requireRoleNotAssignedToCurrentUser(id);
        Set<Long> permissionIds = request.permissionIds();
        if (!permissionIds.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
            boolean allEnabled = permissions.size() == permissionIds.size()
                    && permissions.stream().allMatch(permission -> permission.getStatus() == ENABLED);
            if (!allEnabled) {
                throw new BusinessConflictException("权限不存在或已停用");
            }
        }
        Long operatorId = currentUserId();
        roleMapper.deleteRolePermissions(id);
        permissionIds.forEach(permissionId ->
                roleMapper.insertRolePermission(id, permissionId, operatorId));
        invalidateRoleSessions(id);
        return toResponse(requireRole(id));
    }

    @Override
    public List<PermissionResponse> getPermissions() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getCode))
                .stream().map(converter::toPermissionResponse).toList();
    }

    private RoleAdminResponse toResponse(SysRole role) {
        return converter.toRoleResponse(role, roleMapper.selectPermissionsByRoleId(role.getId()));
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new ResourceNotFoundException("角色不存在");
        }
        return role;
    }

    private void requireRoleNotAssignedToCurrentUser(Long roleId) {
        if (roleMapper.countUserRole(currentUserId(), roleId) > 0) {
            throw new BusinessConflictException("不能修改当前登录账号所使用角色的授权状态");
        }
    }

    private void invalidateRoleSessions(Long roleId) {
        roleMapper.selectUserIdsByRoleId(roleId).forEach(StpUtil::kickout);
    }

    private void requireUniqueCode(String code, Long excludedId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code)
                .ne(excludedId != null, SysRole::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessConflictException("角色编码已存在");
        }
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
