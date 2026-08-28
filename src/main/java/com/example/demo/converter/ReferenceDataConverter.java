package com.example.demo.converter;

import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.dto.DepartmentResponse;
import com.example.demo.dto.TicketCategoryResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.TicketCategory;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataConverter {
    public ReferenceItemResponse toReference(SysDepartment department) {
        return new ReferenceItemResponse(String.valueOf(department.getId()), department.getCode(), department.getName());
    }

    public ReferenceItemResponse toReference(TicketCategory category) {
        return new ReferenceItemResponse(String.valueOf(category.getId()), category.getCode(), category.getName());
    }

    public TicketCategoryResponse toCategoryResponse(TicketCategory category) {
        return new TicketCategoryResponse(
                String.valueOf(category.getId()), category.getCode(), category.getName(),
                category.getDescription(), category.getStatus(), category.getSortOrder(),
                category.getCreateTime(), category.getUpdateTime()
        );
    }

    public DepartmentResponse toDepartmentResponse(SysDepartment department) {
        return new DepartmentResponse(
                String.valueOf(department.getId()), department.getCode(), department.getName(),
                department.getStatus(), department.getSortOrder(),
                department.getCreateTime(), department.getUpdateTime()
        );
    }
}
