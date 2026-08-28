package com.example.demo.converter;

import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.entity.SysPermission;
import com.example.demo.entity.SysRole;
import com.example.demo.mapper.model.PermissionReferenceRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleAdminConverter {
    public RoleAdminResponse toRoleResponse(SysRole role, List<PermissionReferenceRow> permissions) {
        return new RoleAdminResponse(
                String.valueOf(role.getId()), role.getCode(), role.getName(), role.getDescription(),
                role.getStatus(), permissions.stream()
                        .map(permission -> new ReferenceItemResponse(
                                String.valueOf(permission.id()), permission.code(), permission.name()))
                        .toList(),
                role.getCreateTime(), role.getUpdateTime()
        );
    }

    public PermissionResponse toPermissionResponse(SysPermission permission) {
        return new PermissionResponse(
                String.valueOf(permission.getId()), permission.getCode(), permission.getName(),
                permission.getDescription(), permission.getStatus()
        );
    }
}
