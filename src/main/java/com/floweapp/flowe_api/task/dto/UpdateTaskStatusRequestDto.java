package com.floweapp.flowe_api.task.dto;

import com.floweapp.flowe_api.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на изменение статуса задачи")
public record UpdateTaskStatusRequestDto(

        @Schema(
                description = "Новый статус задачи",
                example = "COMPLETED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Требуется статус")
        TaskStatus status
) {}