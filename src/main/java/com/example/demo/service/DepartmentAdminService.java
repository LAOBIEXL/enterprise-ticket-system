package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.CreateDepartmentRequest;
import com.example.demo.dto.DepartmentQuery;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.dto.UpdateDepartmentRequest;
import com.example.demo.dto.UpdateStatusRequest;

public interface DepartmentAdminService {
    PageResponse<DepartmentResponse> page(DepartmentQuery query);

    DepartmentResponse create(CreateDepartmentRequest request);

    DepartmentResponse update(Long id, UpdateDepartmentRequest request);

    DepartmentResponse updateStatus(Long id, UpdateStatusRequest request);
}
