package com.floweapp.flowe_api.finance.dto;

import tools.jackson.databind.JsonNode;

public record UpdateSavingsAccountRequestDto(
        JsonNode name,
        JsonNode targetAmount
) {}
