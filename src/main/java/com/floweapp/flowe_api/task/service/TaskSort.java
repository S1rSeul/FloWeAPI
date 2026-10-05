package com.floweapp.flowe_api.task.service;

import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;

public enum TaskSort {
    CREATED_AT_DESC,
    CREATED_AT_ASC,
    DUE_DATE_DESC,
    DUE_DATE_ASC;

    public static TaskSort parse(String sort, String order) {
        boolean asc;
        if ("asc".equalsIgnoreCase(order)) asc = true;
        else if ("desc".equalsIgnoreCase(order)) asc = false;
        else throw new InvalidQueryParameterException("order должен быть 'asc' или 'desc'");

        if ("createdAt".equalsIgnoreCase(sort)) {
            return asc ? CREATED_AT_ASC : CREATED_AT_DESC;
        }
        if ("dueAt".equalsIgnoreCase(sort)) {
            return asc ? DUE_DATE_ASC : DUE_DATE_DESC;
        }
        throw new InvalidQueryParameterException("sort должен быть 'createdAt' или 'dueAt'");
    }

    public boolean isDueDate() {
        return this == DUE_DATE_ASC || this == DUE_DATE_DESC;
    }
}
