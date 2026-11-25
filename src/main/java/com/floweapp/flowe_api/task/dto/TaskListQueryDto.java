package com.floweapp.flowe_api.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TaskListQueryDto(
        String sort,
        String order,
        @Min(1) @Max(100) Integer limit,
        String cursor
) {
    public TaskListQueryDto {
        sort = (sort == null || sort.isBlank()) ? "createdAt" : sort;
        order = (order == null || order.isBlank()) ? "desc" : order;
    }
}
