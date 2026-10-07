package com.floweapp.flowe_api.task.dto;

import com.floweapp.flowe_api.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequestDto(
        @NotNull(message = "Требуется статус")
        TaskStatus status
) {}
