package com.floweapp.flowe_api.couple.exception;

public class CoupleNotFoundException extends RuntimeException {
    public CoupleNotFoundException(String message) {
        super(message);
    }

    public CoupleNotFoundException() {
        super("Пара пользователя не найдена");
    }
}
