package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateExpenseRequestDto(
        @NotNull(message = "Требуется сумма")
        @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
        BigDecimal amount,

        @NotNull(message = "Требуется обозначить, сколько внес я")
        @DecimalMin(value = "0.00", message = "Моя сумма не может быть отрицательной")
        BigDecimal myPaidAmount,

        @NotNull(message = "Требуется обозначить, сколько внес партнер")
        @DecimalMin(value = "0.00", message = "Сумма партнера не может быть отрицательной")
        BigDecimal partnerPaidAmount,

        @NotNull(message = "Требуется валюта")
        CurrencyCode currency,

        @NotNull(message = "Требуется категория")
        ExpenseCategory category,

        @NotNull(message = "Требуется дата траты")
        OffsetDateTime spentAt,

        @Size(max = 1000, message = "Описание не может быть больше 1000 символов")
        String description
) {}
