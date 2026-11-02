package com.floweapp.flowe_api.calendar.dto;

import tools.jackson.databind.JsonNode;

public record UpdateEventRequestDto(
        JsonNode title,
        JsonNode description,
        JsonNode startsAt,
        JsonNode endsAt,
        JsonNode isShared
        ) {}
