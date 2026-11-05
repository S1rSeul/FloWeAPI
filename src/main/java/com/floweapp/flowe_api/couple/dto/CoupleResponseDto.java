package com.floweapp.flowe_api.couple.dto;

import com.floweapp.flowe_api.couple.entity.CoupleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Данные пространства")
public record CoupleResponseDto(
        @Schema(description = "Идентификатор пространства", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Название пространства", example = "Наше пространство")
        String name,

        @Schema(description = "Статус: pending — ждёт партнёра, active — партнёр присоединился", example = "pending")
        CoupleStatus status,

        @Schema(description = "Имя партнёра; null, пока пространство в статусе pending",
                example = "Malvina", nullable = true)
        String partnerName,

        @Schema(description = "Invite-код; возвращается только в статусе pending, иначе null",
                example = "A1B2C3D4E5", nullable = true)
        String inviteCode
) {}
