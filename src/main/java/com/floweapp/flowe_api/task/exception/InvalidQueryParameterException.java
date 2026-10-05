package com.floweapp.flowe_api.task.exception;

public class InvalidQueryParameterException extends RuntimeException {
    public InvalidQueryParameterException(String message) {
        super(message);
    }
}
