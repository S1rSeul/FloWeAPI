package com.floweapp.flowe_api.auth.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Неверный токен");
    }
}
