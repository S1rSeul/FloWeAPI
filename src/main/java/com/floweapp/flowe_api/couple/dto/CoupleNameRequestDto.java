package com.floweapp.flowe_api.couple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание пространства")
public record CoupleNameRequestDto(
        @Schema(
                description = "Название пространства",
                example = "Наше пространство",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Название пространства не может быть пустым")
        @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
        String name
) {
        public CoupleNameRequestDto {
            if (name != null) {
                name = name.trim();
            }
        }
}
