package com.floweapp.flowe_api.finance.exception;

public class SavingsAccountNotFoundException extends RuntimeException {
    public SavingsAccountNotFoundException() {
        super("Накопительный счет не найден");
    }
}