package com.floweapp.flowe_api.auth.dto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
