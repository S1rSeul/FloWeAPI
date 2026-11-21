package com.floweapp.flowe_api.finance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBudgetRequestDto(
        @NotNull(message = "Требуется сумма лимита")
        @DecimalMin(value = "0.01", message = "Лимит должен быть больше 0")
        @DecimalMax(value = "9999999999.99", message = "Лимит слишком большой")
        BigDecimal limitAmount
) {
}