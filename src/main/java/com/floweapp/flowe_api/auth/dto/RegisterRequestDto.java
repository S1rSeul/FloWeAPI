package com.floweapp.flowe_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на регистрацию пользователя")
public record RegisterRequestDto(
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
        @Size(max = 255)
        String email,

        @Schema(
                description = "Пароль, от 8 до 72 символов",
                example = "StrongPass123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
        String password,

        @Schema(
                description = "Отображаемое имя, от 3 до 100 символов",
                example = "Malvina",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 3, max = 100, message = "Отображаемое имя должно содержать от 3 до 100 символов")
        String displayName
) {
        public RegisterRequestDto {
                if (email != null) {
                        email = email.trim().toLowerCase();
                }

                if (displayName != null) {
                        displayName = displayName.trim();
                }
        }
}