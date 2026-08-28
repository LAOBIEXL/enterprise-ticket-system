package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.ReplaceUserRolesRequest;
import com.example.demo.dto.ResetUserPasswordRequest;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateUserRequest;
import com.example.demo.dto.UserAdminQuery;
import com.example.demo.dto.UserAdminResponse;

public interface UserAdminService {
    PageResponse<UserAdminResponse> page(UserAdminQuery query);

    UserAdminResponse getById(Long id);

    UserAdminResponse create(CreateUserRequest request);

    UserAdminResponse update(Long id, UpdateUserRequest request);

    UserAdminResponse updateStatus(Long id, UpdateStatusRequest request);

    UserAdminResponse replaceRoles(Long id, ReplaceUserRolesRequest request);

    void resetPassword(Long id, ResetUserPasswordRequest request);
}
