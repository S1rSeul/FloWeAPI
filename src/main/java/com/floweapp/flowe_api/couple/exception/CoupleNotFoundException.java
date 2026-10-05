package com.floweapp.flowe_api.couple.exception;

public class CoupleNotFoundException extends RuntimeException {
    public CoupleNotFoundException(String message) {
        super(message);
    }

    public CoupleNotFoundException() {
        super("Пользователь не состоит в паре");
    }
}
