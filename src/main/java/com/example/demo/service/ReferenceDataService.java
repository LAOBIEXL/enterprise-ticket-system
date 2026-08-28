package com.example.demo.service;

import com.example.demo.dto.ReferenceItemResponse;

import java.util.List;

public interface ReferenceDataService {
    List<ReferenceItemResponse> getEnabledDepartments();

    List<ReferenceItemResponse> getEnabledTicketCategories();
}
