package com.floweapp.flowe_api.couple.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoupleNameRequestDto(
        @NotBlank(message = "Требуется название пространства")
        @Size(min = 1, max = 100, message = "Имя должно быть от 1 до 100 символов")
        String name
) {
        public CoupleNameRequestDto {
            if (name != null) {
                name = name.trim();
            }
        }
}
