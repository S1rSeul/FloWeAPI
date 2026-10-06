package com.floweapp.flowe_api.task.service;

import com.floweapp.flowe_api.task.entity.TaskStatus;
import com.floweapp.flowe_api.task.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class StatusTransitionValidator {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED.put(TaskStatus.todo, EnumSet.of(
                TaskStatus.in_progress,
                TaskStatus.done,
                TaskStatus.closed
        ));
        ALLOWED.put(TaskStatus.in_progress, EnumSet.of(
                TaskStatus.todo,
                TaskStatus.done,
                TaskStatus.closed
        ));
        ALLOWED.put(TaskStatus.done, EnumSet.of(
                TaskStatus.todo,
                TaskStatus.in_progress
        ));
        ALLOWED.put(TaskStatus.closed, EnumSet.of(
                TaskStatus.todo
        ));
    }

    public void validate(TaskStatus from, TaskStatus to) {
        Set<TaskStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(TaskStatus.class));
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}
