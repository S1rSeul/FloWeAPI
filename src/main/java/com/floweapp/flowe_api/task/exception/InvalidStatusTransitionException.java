package com.floweapp.flowe_api.task.exception;

import com.floweapp.flowe_api.task.entity.TaskStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(TaskStatus from, TaskStatus to) {
        super("Невозможно сменить статус задачи с '" + from + "' на '" + to + "'");
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
