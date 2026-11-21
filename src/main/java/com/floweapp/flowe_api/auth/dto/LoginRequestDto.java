package com.floweapp.flowe_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Запрос на вход")
public record LoginRequestDto(
        @Schema(
                description = "Email пользователя",
                example = "buratino@malvina.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email не может быть пустым")
        @Pattern(
                regexp = "^[^@\\s]+@[^@.\\s]+(?:\\.[^@.\\s]+)+$",
                message = "Email должен иметь формат адреса электронной почты"
        )
        String email,

        @Schema(
                description = "Пароль пользователя",
                example = "StrongPass123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Пароль не может быть пустым")
        String password
) {
    public LoginRequestDto {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
