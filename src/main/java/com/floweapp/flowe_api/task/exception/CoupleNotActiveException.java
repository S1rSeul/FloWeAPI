package com.floweapp.flowe_api.task.exception;

public class CoupleNotActiveException extends RuntimeException {
    public CoupleNotActiveException() {
        super("Пара пользователя неактивна");
    }

    public CoupleNotActiveException(String message) {
        super(message);
    }
}
