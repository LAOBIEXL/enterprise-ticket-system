package com.example.demo.mapper.model;

/** Mapper 使用的查询条件，调用方负责注入数据范围。 */
public record TicketQueryCriteria(
        String status,
        Long categoryId,
        Long requesterId,
        Long assigneeId,
        String keyword
) {
}
