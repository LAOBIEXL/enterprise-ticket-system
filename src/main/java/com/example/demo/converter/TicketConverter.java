package com.example.demo.converter;

import com.example.demo.dto.TicketDetailResponse;
import com.example.demo.dto.TicketRecordResponse;
import com.example.demo.dto.TicketSummaryResponse;
import com.example.demo.mapper.model.TicketRecordRow;
import com.example.demo.mapper.model.TicketViewRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketConverter {

    public TicketSummaryResponse toSummary(TicketViewRow row) {
        return new TicketSummaryResponse(
                id(row.getId()), row.getTicketNo(), row.getTitle(), row.getStatus(),
                id(row.getCategoryId()), row.getCategoryName(),
                id(row.getRequesterId()), row.getRequesterName(), row.getRequesterDepartmentName(),
                id(row.getAssigneeId()), row.getAssigneeName(), row.getCreateTime(), row.getUpdateTime()
        );
    }

    public TicketDetailResponse toDetail(TicketViewRow row, List<TicketRecordRow> recordRows) {
        List<TicketRecordResponse> records = recordRows.stream().map(this::toRecord).toList();
        return new TicketDetailResponse(
                id(row.getId()), row.getTicketNo(), row.getTitle(), row.getDescription(), row.getStatus(),
                id(row.getCategoryId()), row.getCategoryName(),
                id(row.getRequesterId()), row.getRequesterName(), row.getRequesterDepartmentName(),
                id(row.getAssigneeId()), row.getAssigneeName(), row.getVersion(),
                row.getResolvedTime(), row.getClosedTime(), row.getCreateTime(), row.getUpdateTime(), records
        );
    }

    private TicketRecordResponse toRecord(TicketRecordRow row) {
        return new TicketRecordResponse(
                id(row.getId()), row.getAction(), row.getFromStatus(), row.getToStatus(), row.getContent(),
                id(row.getOperatorId()), row.getOperatorName(),
                id(row.getTargetUserId()), row.getTargetUserName(), row.getCreateTime()
        );
    }

    private String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
