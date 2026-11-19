package com.floweapp.flowe_api.calendar.controller;

import com.floweapp.flowe_api.calendar.dto.CreateEventRequestDto;
import com.floweapp.flowe_api.calendar.dto.EventResponseDto;
import com.floweapp.flowe_api.calendar.dto.UpdateEventRequestDto;
import com.floweapp.flowe_api.calendar.service.EventService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventResponseDto create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateEventRequestDto request
    ) {
        return eventService.createEvent(currentUser, request);
    }

    @GetMapping("/{id}")
    public EventResponseDto getById(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        return eventService.getEvent(currentUser, id);
    }

    @PatchMapping("/{id}")
    public EventResponseDto update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestBody UpdateEventRequestDto request
    ) {
        return eventService.updateEvent(currentUser, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        eventService.deleteEvent(currentUser, id);
    }
}