package com.floweapp.flowe_api.task.exception;

public class TaskNotFoundException extends RuntimeException {
  public TaskNotFoundException() {
    super("Задача не найдена");
  }

  public TaskNotFoundException(String message) {
        super(message);
    }
}
