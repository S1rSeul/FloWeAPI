package com.floweapp.flowe_api.calendar.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record CalendarRangeRequestDto(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
) {
    @AssertTrue(message = "from должно быть не позже to")
    public boolean isRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}