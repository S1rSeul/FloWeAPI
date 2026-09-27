package com.floweapp.flowe_api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank
        String refreshToken
) {}
