package com.example.demo.dto;

public record PermissionResponse(
        String id,
        String code,
        String name,
        String description,
        Integer status
) {
}
