package com.floweapp.flowe_api.task.dto;

import com.floweapp.flowe_api.task.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskResponseDto(
        UUID id,
        String title,
        String description,
        UUID createdById,
        boolean assignedToMe,
        boolean assignedToPartner,
        OffsetDateTime dueDate,
        TaskStatus status,
        boolean isOverdue,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
