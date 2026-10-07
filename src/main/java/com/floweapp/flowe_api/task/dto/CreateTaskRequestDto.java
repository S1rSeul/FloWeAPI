package com.floweapp.flowe_api.task.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateTaskRequestDto(
        @NotBlank(message = "Требуется название")
        @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
        String title,

        @Size(max = 2000, message = "Размер описания не должен превышать 2000 символов")
        String description,

        boolean assignedToMe,

        boolean assignedToPartner,

        @Future(message = "Дата дедлайна должна быть в будущем времени")
        OffsetDateTime dueDate
) {
        public CreateTaskRequestDto {
                if (title != null) {
                        title = title.trim();
                }
        }
}
