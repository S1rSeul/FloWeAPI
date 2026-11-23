package com.floweapp.flowe_api.finance.exception;

public class InvalidBudgetException extends RuntimeException {
    public InvalidBudgetException(String message) {
        super(message);
    }
}