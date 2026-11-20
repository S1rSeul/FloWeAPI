package finance.dto;

import finance.entity.CurrencyCode;

import java.math.BigDecimal;

public record CurrencySummaryDto(
        CurrencyCode currency,
        BigDecimal totalAmount,
        BigDecimal myPaidAmount,
        BigDecimal partnerPaidAmount
) {}
