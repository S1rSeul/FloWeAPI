package com.floweapp.flowe_api.calendar.dto;

import com.floweapp.flowe_api.calendar.entity.Event;
import com.floweapp.flowe_api.couple.entity.Couple;
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
        // Поля для событий (личное / партнера)
        boolean isMyPersonal,
        boolean isPartnerPersonal,
        // Поля для задач (назначено на меня / на партнера)
        boolean assignedToMe,
        boolean assignedToPartner,
        // Статус и просрочка (для задач)
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
                false, // assignedToMe не применимо для событий
                false, // assignedToPartner не применимо для событий

                false, // isOverdue не применимо для событий
                null   // status не применим для событий
        );
    }

    public static CalendarItemDto fromTask(Task task, Couple couple, UUID currentUserId) {
        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Задача должна иметь dueDate чтобы находиться в календаре");
        }

        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUserId);

        boolean assignedToMe = isCurrentUserUser1
                ? task.isUser1Assignee()
                : task.isUser2Assignee();

        boolean assignedToPartner = isCurrentUserUser1
                ? task.isUser2Assignee()
                : task.isUser1Assignee();

        boolean isOverdue = task.getDueDate().isBefore(OffsetDateTime.now())
                && task.getStatus() != TaskStatus.done
                && task.getStatus() != TaskStatus.closed;

        return new CalendarItemDto(
                task.getId(),
                CalendarItemType.task_deadline,
                task.getTitle(),
                task.getDueDate(),
                null,
                false, // isMyPersonal не применимо для задач (задачи всегда общие)
                false, // isPartnerPersonal не применимо для задач
                assignedToMe,
                assignedToPartner,
                isOverdue,
                task.getStatus()
        );
    }
}
