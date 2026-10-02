package com.floweapp.flowe_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDto(
        @NotBlank
        @Email
        @Pattern(regexp = "^[^@\\s]+@[^@.\\s]+(?:\\.[^@.\\s]+)+$")
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
