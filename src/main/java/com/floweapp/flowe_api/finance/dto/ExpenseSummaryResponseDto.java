package com.floweapp.flowe_api.finance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExpenseSummaryResponseDto(
        OffsetDateTime from,
        OffsetDateTime to,
        List<CurrencySummaryDto> summaries
) {}
