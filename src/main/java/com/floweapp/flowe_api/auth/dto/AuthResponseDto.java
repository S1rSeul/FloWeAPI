package com.floweapp.flowe_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ после успешной регистрации/аутентификации")
public record AuthResponseDto(
        @Schema(description = "JWT access token для авторизации запросов")
        String accessToken,
        @Schema(description = "Refresh token для получения нового access token")
        String refreshToken,
        @Schema(description = "Время жизни access token в секундах")
        long expiresIn
) {}
