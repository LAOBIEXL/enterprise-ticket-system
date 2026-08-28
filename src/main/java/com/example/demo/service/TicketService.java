package com.example.demo.service;

import com.example.demo.common.PageResponse;
import com.example.demo.dto.CreateTicketRequest;
import com.example.demo.dto.AddTicketRecordRequest;
import com.example.demo.dto.AssignTicketRequest;
import com.example.demo.dto.ConfirmTicketRequest;
import com.example.demo.dto.ReassignTicketRequest;
import com.example.demo.dto.ResolveTicketRequest;
import com.example.demo.dto.ReturnTicketRequest;
import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketQuery;
import com.example.demo.dto.TicketSummaryResponse;

public interface TicketService {
    TicketDetailResponse create(CreateTicketRequest request, String idempotencyKey);

    PageResponse<TicketSummaryResponse> getMine(TicketQuery query);

    PageResponse<TicketSummaryResponse> getAssigned(TicketQuery query);

    PageResponse<TicketSummaryResponse> getAll(TicketQuery query);

    TicketDetailResponse getDetail(Long id);

    TicketDetailResponse assign(Long id, AssignTicketRequest request);

    TicketDetailResponse reassign(Long id, ReassignTicketRequest request);

    TicketDetailResponse start(Long id);

    TicketDetailResponse addRecord(Long id, AddTicketRecordRequest request);

    TicketDetailResponse resolve(Long id, ResolveTicketRequest request);

    TicketDetailResponse confirm(Long id, ConfirmTicketRequest request);

    TicketDetailResponse returnForRework(Long id, ReturnTicketRequest request);
}
