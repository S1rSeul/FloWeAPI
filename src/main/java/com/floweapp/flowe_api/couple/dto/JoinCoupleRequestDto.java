package com.floweapp.flowe_api.couple.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinCoupleRequestDto(
        @NotBlank(message = "Требуется invite код")
        @Size(min = 10, max = 10, message = "Invite-код должен быть ровно из 10 символов")
        String inviteCode
) {
    public JoinCoupleRequestDto {
        if (inviteCode != null) {
            inviteCode = inviteCode.trim().toUpperCase();
        }
    }
}
