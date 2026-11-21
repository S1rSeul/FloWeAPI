package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;

import java.math.BigDecimal;

public record CurrencySummaryDto(
        CurrencyCode currency,
        BigDecimal totalAmount,
        BigDecimal myPaidAmount,
        BigDecimal partnerPaidAmount
) {}
