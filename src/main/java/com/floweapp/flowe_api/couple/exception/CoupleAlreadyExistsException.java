package com.floweapp.flowe_api.couple.exception;

public class CoupleAlreadyExistsException extends RuntimeException {
    public CoupleAlreadyExistsException(String message) {
        super(message);
    }

    public CoupleAlreadyExistsException() {
        super("Пользователь уже находится в паре");
    }
}
