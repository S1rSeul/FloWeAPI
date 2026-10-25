package com.floweapp.flowe_api.calendar.dto;

import com.floweapp.flowe_api.calendar.entity.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponseDto(
        UUID id,
        String title,
        String description,
        UUID createdById,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean isShared,
        boolean isMyPersonal,
        boolean isPartnerPersonal,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static EventResponseDto from(Event event, UUID currentUserId) {
        boolean isMyPersonal = !event.isShared() && event.getCreatedById().equals(currentUserId);
        boolean isPartnerPersonal = !event.isShared() && !event.getCreatedById().equals(currentUserId);

        return new EventResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCreatedById(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.isShared(),
                isMyPersonal,
                isPartnerPersonal,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
