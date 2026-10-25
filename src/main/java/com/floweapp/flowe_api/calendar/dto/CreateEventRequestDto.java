package com.floweapp.flowe_api.calendar.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateEventRequestDto(
        @NotBlank(message = "Название не может быть пустым")
        @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
        String title,

        @Size(max = 2000, message = "Описание не может быть больше 2000 символов")
        String description,

        @NotNull(message = "Время начала события обязательно")
        @Future(message = "Время начала события должно быть в будущем времени")
        OffsetDateTime startsAt,

        @Future(message = "Время конца события должно быть в будущем времени")
        OffsetDateTime endsAt,

        Boolean isShared
) {}
