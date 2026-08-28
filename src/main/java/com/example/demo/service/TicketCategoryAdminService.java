package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.CreateTicketCategoryRequest;
import com.example.demo.dto.TicketCategoryQuery;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.dto.UpdateStatusRequest;
import com.example.demo.dto.UpdateTicketCategoryRequest;

public interface TicketCategoryAdminService {
    PageResponse<TicketCategoryResponse> page(TicketCategoryQuery query);

    TicketCategoryResponse create(CreateTicketCategoryRequest request);

    TicketCategoryResponse update(Long id, UpdateTicketCategoryRequest request);

    TicketCategoryResponse updateStatus(Long id, UpdateStatusRequest request);
}
