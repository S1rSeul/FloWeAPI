package com.floweapp.flowe_api.finance.dto;

import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.SavingsAccount;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SavingsAccountResponseDto(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentBalance,
        CurrencyCode currency,
        BigDecimal progress, // % (0-100)
        OffsetDateTime createdAt
) {
    public static SavingsAccountResponseDto from(SavingsAccount account) {
        return new SavingsAccountResponseDto(
                account.getId(),
                account.getName(),
                account.getTargetAmount(),
                account.getCurrentBalance(),
                account.getCurrency(),
                account.getProgress(),
                account.getCreatedAt()
        );
    }
}
