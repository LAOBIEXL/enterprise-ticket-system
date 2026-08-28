package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketQuery;
import com.example.demo.dto.TicketSummaryResponse;

public interface TicketService {
    TicketDetailResponse create(CreateTicketRequest request, String idempotencyKey);

    PageResponse<TicketSummaryResponse> getMine(TicketQuery query);

    PageResponse<TicketSummaryResponse> getAssigned(TicketQuery query);

    PageResponse<TicketSummaryResponse> getAll(TicketQuery query);

    TicketDetailResponse getDetail(Long id);
}
