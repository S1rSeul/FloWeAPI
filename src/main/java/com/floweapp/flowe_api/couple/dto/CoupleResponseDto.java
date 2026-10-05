package com.floweapp.flowe_api.couple.dto;

import com.floweapp.flowe_api.couple.entity.CoupleStatus;

import java.util.UUID;

public record CoupleResponseDto(
        UUID id,
        String name,
        CoupleStatus status,
        String partnerName,
        String inviteCode
) {}
