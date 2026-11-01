package com.floweapp.flowe_api.calendar.dto;

import com.floweapp.flowe_api.calendar.entity.Event;
import com.floweapp.flowe_api.task.entity.Task;
import com.floweapp.flowe_api.task.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CalendarItemDto(
        UUID id,
        CalendarItemType type,
        String title,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean isMyPersonal,
        boolean isPartnerPersonal,
        boolean isOverdue,
        TaskStatus status
) {

    public static CalendarItemDto fromEvent(Event event, UUID currentUserId) {
        boolean isMyPersonal = !event.isShared() && event.getCreatedById().equals(currentUserId);
        boolean isPartnerPersonal = !event.isShared() && !event.getCreatedById().equals(currentUserId);

        return new CalendarItemDto(
                event.getId(),
                CalendarItemType.event,
                event.getTitle(),
                event.getStartsAt(),
                event.getEndsAt(),
                isMyPersonal,
                isPartnerPersonal,
                false,
                null
        );
    }

    public static CalendarItemDto fromTask(Task task) {
        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Таска должна иметь dueDate чтобы находиться в календаре");
        }

        boolean isOverdue = task.getDueDate().isBefore(OffsetDateTime.now())
                && task.getStatus() != TaskStatus.done
                && task.getStatus() != TaskStatus.closed;

        return new CalendarItemDto(
                task.getId(),
                CalendarItemType.task_deadline,
                task.getTitle(),
                task.getDueDate(),
                null,
                false,
                false,
                isOverdue,
                task.getStatus()
        );
    }
}
