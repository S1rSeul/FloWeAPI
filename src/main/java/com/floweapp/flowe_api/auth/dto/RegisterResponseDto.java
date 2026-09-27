package com.floweapp.flowe_api.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterResponseDto(
        UUID id,
        String email,
        String displayName,
        OffsetDateTime createdAt
) {
}