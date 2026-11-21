package com.floweapp.flowe_api.finance.dto;

import tools.jackson.databind.JsonNode;

public record UpdateExpenseRequestDto(
        JsonNode amount,
        JsonNode myPaidAmount,
        JsonNode partnerPaidAmount,
        JsonNode currency,
        JsonNode category,
        JsonNode spentAt,
        JsonNode description
) {}
