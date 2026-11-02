package com.floweapp.flowe_api.calendar.controller;

import com.floweapp.flowe_api.calendar.dto.CreateEventRequestDto;
import com.floweapp.flowe_api.calendar.dto.EventResponseDto;
import com.floweapp.flowe_api.calendar.dto.UpdateEventRequestDto;
import com.floweapp.flowe_api.calendar.service.EventService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateEventRequestDto request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(currentUser, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EventResponseDto> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestBody UpdateEventRequestDto request
            ) {
        return ResponseEntity.ok(eventService.updateEvent(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        eventService.deleteEvent(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
