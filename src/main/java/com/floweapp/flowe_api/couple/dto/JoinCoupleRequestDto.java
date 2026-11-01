package com.floweapp.flowe_api.couple.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на присоединение к пространству")
public record JoinCoupleRequestDto(
        @Schema(
                description = "10-символьный invite-код; регистр не важен",
                example = "A1B2C3D4E5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Invite код не может быть пустым")
        @Size(min = 10, max = 10, message = "Invite-код должен быть ровно из 10 символов")
        String inviteCode
) {
    public JoinCoupleRequestDto {
        if (inviteCode != null) {
            inviteCode = inviteCode.trim().toUpperCase();
        }
    }
}
