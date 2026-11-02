package com.floweapp.flowe_api.calendar.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException() {
        super("Событие не найдено");
    }
}
