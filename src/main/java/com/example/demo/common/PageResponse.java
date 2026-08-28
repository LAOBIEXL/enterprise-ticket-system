package com.example.demo.common;

import java.util.List;

/**
 * 不暴露持久层分页对象的统一分页响应。
 */
public record PageResponse<T>(
        List<T> records,
        long total,
        long pageNum,
        long pageSize,
        long pages
) {
    public static <T> PageResponse<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(List.copyOf(records), total, pageNum, pageSize, pages);
    }
}
