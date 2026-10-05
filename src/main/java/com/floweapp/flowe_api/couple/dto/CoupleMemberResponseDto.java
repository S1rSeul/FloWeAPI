package com.floweapp.flowe_api.couple.dto;

import java.util.UUID;

public record CoupleMemberResponseDto(
        UUID id,
        String displayName,
        boolean isMe
) {}
