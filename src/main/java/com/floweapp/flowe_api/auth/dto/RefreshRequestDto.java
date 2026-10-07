package com.floweapp.flowe_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на обновление или удаление сессии")
public record RefreshRequestDto(
        @Schema(
                description = "Refresh token",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token не может быть пустым")
        String refreshToken
) {}
