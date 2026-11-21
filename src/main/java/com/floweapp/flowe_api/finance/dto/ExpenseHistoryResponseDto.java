package com.floweapp.flowe_api.finance.dto;

import java.util.List;

public record ExpenseHistoryResponseDto(
        List<ExpenseResponseDto> items,
        String nextCursor,
        boolean hasMore,
        List<CurrencySummaryDto> totalSummary
) {}
