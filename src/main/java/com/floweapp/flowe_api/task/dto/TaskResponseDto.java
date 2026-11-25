package com.floweapp.flowe_api.task.dto;

import com.floweapp.flowe_api.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Данные задачи общего пространства")
public record TaskResponseDto(

        @Schema(
                description = "Идентификатор задачи",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID id,

        @Schema(
                description = "Название задачи",
                example = "Купить продукты"
        )
        String title,

        @Schema(
                description = "Описание задачи; может отсутствовать",
                example = "Молоко, хлеб, сыр",
                nullable = true
        )
        String description,

        @Schema(
                description = "Идентификатор пользователя, создавшего задачу",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID createdById,

        @Schema(
                description = "Признак, что задача назначена текущему авторизованному пользователю",
                example = "true"
        )
        boolean assignedToMe,

        @Schema(
                description = "Признак, что задача назначена партнёру текущего пользователя",
                example = "false"
        )
        boolean assignedToPartner,

        @Schema(
                description = "Дедлайн в ISO-8601 с timezone offset; null, если дедлайн не установлен",
                example = "2026-10-15T18:00:00+04:00",
                format = "date-time",
                nullable = true
        )
        OffsetDateTime dueDate,

        @Schema(
                description = "Текущий статус задачи",
                example = "todo",
                allowableValues = {
                        "todo",
                        "in_progress",
                        "done",
                        "closed"
                }
        )
        TaskStatus status,

        @Schema(
                description = """
                        Признак просроченной задачи.
                        true, если дедлайн уже наступил, а статус не done и не closed.
                        """,
                example = "false"
        )
        boolean isOverdue,

        @Schema(
                description = "Время завершения; заполняется при переводе задачи в статус done",
                example = "2026-10-12T10:30:00+04:00",
                format = "date-time",
                nullable = true
        )
        OffsetDateTime completedAt,

        @Schema(
                description = "Время создания задачи",
                example = "2026-10-11T21:23:00+04:00",
                format = "date-time"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Время последнего изменения задачи",
                example = "2026-10-11T21:25:00+04:00",
                format = "date-time"
        )
        OffsetDateTime updatedAt
) {}