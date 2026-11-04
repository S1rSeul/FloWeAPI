package com.floweapp.flowe_api.calendar.service;

import com.floweapp.flowe_api.calendar.dto.CreateEventRequestDto;
import com.floweapp.flowe_api.calendar.dto.EventResponseDto;
import com.floweapp.flowe_api.calendar.dto.UpdateEventRequestDto;
import com.floweapp.flowe_api.calendar.entity.Event;
import com.floweapp.flowe_api.calendar.exception.EventNotFoundException;
import com.floweapp.flowe_api.calendar.repository.EventRepository;
import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public EventResponseDto createEvent(User currentUser, CreateEventRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());

        if (request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Время конца события должно быть после время начала");
        }

        Event event = Event.builder()
                .coupleId(couple.getId())
                .createdById(currentUser.getId())
                .title(request.title().trim())
                .description(request.description())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .isShared(request.isShared() != null ? request.isShared() : true)
                .build();

        Event saved = eventRepository.save(event);
        return EventResponseDto.from(saved, currentUser.getId());
    }

    @Transactional
    public EventResponseDto updateEvent(User currentUser, UUID eventId, UpdateEventRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());

        Event event = eventRepository.findByIdAndCoupleId(eventId, couple.getId())
                .orElseThrow(EventNotFoundException::new);

        if (request.title() != null && !request.title().isNull()) {
            String newTitle = request.title().asString().trim();
            if (newTitle.isEmpty() || newTitle.length() > 100) {
                throw new InvalidQueryParameterException("Название должно быть от 1 до 100 символов");
            }
            event.setTitle(newTitle.trim());
        }

        if (request.description() != null) {
            if (request.description().asString().length() > 2000) {
                throw new InvalidQueryParameterException("Размер описания не должен превышать 2000 символов");
            }
            event.setDescription(request.description().isNull() ? null : request.description().asString());
        }

        if (request.startsAt() != null) {
            if (request.startsAt().isNull()) {
                throw new InvalidQueryParameterException("Время начала события обязательно");
            }
            OffsetDateTime parsed = OffsetDateTime.parse(request.startsAt().asString());
            if (parsed.isBefore(OffsetDateTime.now())) {
                throw new InvalidQueryParameterException("Время начала события должно быть в будущем времени");
            }
            event.setStartsAt(parsed);
        }

        if (request.endsAt() != null) {
            if (request.endsAt().isNull()) {
                event.setEndsAt(null);
            }
            OffsetDateTime parsed = OffsetDateTime.parse(request.endsAt().asString());
            if (parsed.isBefore(OffsetDateTime.now())) {
                throw new InvalidQueryParameterException("Время конца события должно быть в будущем времени");
            }
            event.setEndsAt(parsed);
        }

        if (request.isShared() != null) {
            if (request.isShared().isNull()) {
                event.setShared(true);
            }
            event.setShared(request.isShared().asBoolean());
        }

        if (event.getEndsAt() != null && !event.getEndsAt().isAfter(event.getStartsAt())) {
            throw new IllegalArgumentException("Время конца события должно быть после время начала");
        }

        Event saved = eventRepository.save(event);
        return EventResponseDto.from(saved, currentUser.getId());
    }

    @Transactional
    public void deleteEvent(User currentUser, UUID eventId) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());

        Event event = eventRepository.findByIdAndCoupleId(eventId, couple.getId())
                .orElseThrow(EventNotFoundException::new);

        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEvent(User currentUser, UUID eventId) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());

        Event event = eventRepository.findByIdAndCoupleId(eventId, couple.getId())
                .orElseThrow(EventNotFoundException::new);

        return EventResponseDto.from(event, currentUser.getId());
    }

    private Couple getActiveCoupleOrThrow(UUID userId) {
        Couple couple = coupleRepository.findByUserId(userId)
                .orElseThrow(CoupleNotFoundException::new);
        if (!couple.isActive()) {
            throw new CoupleNotActiveException();
        }
        return couple;
    }
}
