package com.floweapp.flowe_api.calendar.controller;

import com.floweapp.flowe_api.calendar.dto.CalendarItemDto;
import com.floweapp.flowe_api.calendar.dto.CalendarRangeRequestDto;
import com.floweapp.flowe_api.calendar.service.CalendarService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public List<CalendarItemDto> getCalendar(
            @AuthenticationPrincipal User currentUser,
            @Valid CalendarRangeRequestDto range
    ) {
        return calendarService.getCalendar(currentUser, range.from(), range.to());
    }
}