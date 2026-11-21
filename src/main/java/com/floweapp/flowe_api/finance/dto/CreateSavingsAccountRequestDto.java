package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSavingsAccountRequestDto(
        @NotBlank @Size(max = 100) String name,
        @DecimalMin("0.01") BigDecimal targetAmount,
        @NotNull CurrencyCode currency
) {}
