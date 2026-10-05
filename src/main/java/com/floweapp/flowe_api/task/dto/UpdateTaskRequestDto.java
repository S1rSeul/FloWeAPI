package com.floweapp.flowe_api.task.dto;

import tools.jackson.databind.JsonNode;

public record UpdateTaskRequestDto(
        JsonNode title,
        JsonNode description,
        JsonNode assignedToMe,
        JsonNode assignedToPartner,
        JsonNode dueDate
) {}
