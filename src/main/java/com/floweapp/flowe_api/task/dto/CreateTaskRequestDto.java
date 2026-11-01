package com.floweapp.flowe_api.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Schema(description = "Запрос на создание задачи общего пространства")
public record CreateTaskRequestDto(

        @Schema(
                description = "Название задачи",
                example = "Купить продукты",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Требуется название")
        @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
        String title,

        @Schema(
                description = "Дополнительное описание задачи",
                example = "Молоко, хлеб, сыр",
                nullable = true
        )
        @Size(max = 2000, message = "Размер описания не должен превышать 2000 символов")
        String description,

        @Schema(
                description = "Назначить задачу текущему пользователю",
                example = "true",
                defaultValue = "false"
        )
        boolean assignedToMe,

        @Schema(
                description = "Назначить задачу партнёру",
                example = "false",
                defaultValue = "false"
        )
        boolean assignedToPartner,

        @Schema(
                description = "Дедлайн задачи в формате ISO-8601; дата должна быть в будущем",
                example = "2026-10-15T18:00:00+04:00",
                format = "date-time",
                nullable = true
        )
        @Future(message = "Дата дедлайна должна быть в будущем времени")
        OffsetDateTime dueDate
) {
        public CreateTaskRequestDto {
                if (title != null) {
                        title = title.trim();
                }
        }
}