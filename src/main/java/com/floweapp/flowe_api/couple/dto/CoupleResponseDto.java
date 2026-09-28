package com.floweapp.flowe_api.couple.dto;

import com.floweapp.flowe_api.couple.entity.CoupleStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CoupleResponseDto(
        UUID id,
        String name,
        CoupleStatus status,
        UUID user1Id,
        UUID user2Id,
        String inviteCode,
        OffsetDateTime createdAt
) {}
