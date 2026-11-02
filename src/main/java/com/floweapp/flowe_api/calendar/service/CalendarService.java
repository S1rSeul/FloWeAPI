package com.floweapp.flowe_api.calendar.service;

import com.floweapp.flowe_api.calendar.dto.CalendarItemDto;
import com.floweapp.flowe_api.calendar.entity.Event;
import com.floweapp.flowe_api.calendar.repository.EventRepository;
import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.task.entity.Task;
import com.floweapp.flowe_api.task.repository.TaskRepository;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final EventRepository eventRepository;
    private final CoupleRepository coupleRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<CalendarItemDto> getCalendar(User currentUser, OffsetDateTime from, OffsetDateTime to) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());

        List<Event> events = eventRepository.findInRange(couple.getId(), from, to);

        List<Task> tasks = taskRepository.findTaskDeadlinesInRange(couple.getId(), from, to);

        List<CalendarItemDto> items = new ArrayList<>();
        events.forEach(e -> items.add(CalendarItemDto.fromEvent(e, currentUser.getId())));
        tasks.forEach(t -> items.add(CalendarItemDto.fromTask(t, couple, currentUser.getId())));

        items.sort(Comparator.comparing(CalendarItemDto::startsAt));

        return items;
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
