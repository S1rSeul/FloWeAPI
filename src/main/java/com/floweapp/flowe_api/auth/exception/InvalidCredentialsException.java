package com.floweapp.flowe_api.auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Неверный email или пароль");
    }
}
