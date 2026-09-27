package com.floweapp.flowe_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotBlank
        @Size(min = 3, max = 100)
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