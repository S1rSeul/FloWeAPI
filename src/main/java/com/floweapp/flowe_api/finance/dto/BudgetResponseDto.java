package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.BudgetPeriod;
import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponseDto(
        UUID id,
        ExpenseCategory category,
        UUID savingsAccountId,
        String savingsAccountName,
        BigDecimal limitAmount,
        CurrencyCode currency,
        BudgetPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal usagePercent,
        boolean isExceeded
) {
}