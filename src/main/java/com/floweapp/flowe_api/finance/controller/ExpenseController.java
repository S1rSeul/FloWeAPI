package com.floweapp.flowe_api.finance.controller;

import com.floweapp.flowe_api.finance.dto.*;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.finance.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateExpenseRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(currentUser, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponseDto> getSummary(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ResponseEntity.ok(expenseService.getSummary(currentUser, from, to));
    }

    @GetMapping
    public ResponseEntity<ExpenseHistoryResponseDto> getExpenses(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(expenseService.getExpenses(currentUser, from, to, category, limit, cursor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(expenseService.getExpense(currentUser, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestBody UpdateExpenseRequestDto request
    ) {
        return ResponseEntity.ok(expenseService.updateExpense(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        expenseService.deleteExpense(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}