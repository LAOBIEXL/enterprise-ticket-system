package com.example.demo.service;

import com.example.demo.dto.CreateRoleRequest;
import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.ReplaceRolePermissionsRequest;
import com.example.demo.dto.RoleAdminResponse;
import com.example.demo.dto.UpdateRoleRequest;
import com.example.demo.dto.UpdateStatusRequest;

import java.util.List;

public interface RolePermissionAdminService {
    List<RoleAdminResponse> getRoles();

    RoleAdminResponse createRole(CreateRoleRequest request);

    RoleAdminResponse updateRole(Long id, UpdateRoleRequest request);

    RoleAdminResponse updateRoleStatus(Long id, UpdateStatusRequest request);

    RoleAdminResponse replacePermissions(Long id, ReplaceRolePermissionsRequest request);

    List<PermissionResponse> getPermissions();
}
