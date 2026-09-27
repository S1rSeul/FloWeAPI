package com.floweapp.flowe_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
    public LoginRequestDto {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
