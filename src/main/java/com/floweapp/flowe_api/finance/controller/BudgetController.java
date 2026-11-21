package com.floweapp.flowe_api.finance.controller;

import com.floweapp.flowe_api.finance.dto.BudgetResponseDto;
import com.floweapp.flowe_api.finance.dto.CreateBudgetRequestDto;
import com.floweapp.flowe_api.finance.dto.UpdateBudgetRequestDto;
import com.floweapp.flowe_api.finance.service.BudgetService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public BudgetResponseDto create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateBudgetRequestDto request
    ) {
        return budgetService.create(currentUser, request);
    }

    @GetMapping
    public List<BudgetResponseDto> getAll(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return budgetService.getAll(currentUser, month);
    }

    @GetMapping("/{id}")
    public BudgetResponseDto getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return budgetService.getById(currentUser, id, month);
    }

    @PatchMapping("/{id}")
    public BudgetResponseDto update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequestDto request
    ) {
        return budgetService.update(currentUser, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        budgetService.delete(currentUser, id);
    }
}