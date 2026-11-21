package com.floweapp.flowe_api.finance.exception;

public class ExpenseNotFoundException extends RuntimeException {
    public ExpenseNotFoundException() {
        super("Расход не найден");
    }
}