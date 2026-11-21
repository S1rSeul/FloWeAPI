package com.floweapp.flowe_api.finance.exception;

public class BudgetNotFoundException extends RuntimeException {
    public BudgetNotFoundException() {
        super("Лимит бюджета не найден");
    }
}