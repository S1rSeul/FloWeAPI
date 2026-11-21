package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBudgetRequestDto(
        ExpenseCategory category,
        UUID savingsAccountId,

        @NotNull(message = "Требуется сумма лимита")
        @DecimalMin(value = "0.01", message = "Лимит должен быть больше 0")
        @DecimalMax(value = "9999999999.99", message = "Лимит слишком большой")
        BigDecimal limitAmount,

        @NotNull(message = "Требуется валюта")
        CurrencyCode currency
) {
    @AssertTrue(message = "Укажите ровно одно: category или savingsAccountId")
    public boolean isTargetValid() {
        return (category == null) == (savingsAccountId != null);
    }
}