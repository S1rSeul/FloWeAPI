package com.floweapp.flowe_api.finance.controller;

import com.floweapp.flowe_api.finance.dto.CreateSavingsAccountRequestDto;
import com.floweapp.flowe_api.finance.dto.DepositRequestDto;
import com.floweapp.flowe_api.finance.dto.SavingsAccountResponseDto;
import com.floweapp.flowe_api.finance.dto.UpdateSavingsAccountRequestDto;
import com.floweapp.flowe_api.finance.service.SavingsService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    @PostMapping
    public ResponseEntity<SavingsAccountResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateSavingsAccountRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingsService.create(currentUser, request));
    }

    @GetMapping
    public ResponseEntity<List<SavingsAccountResponseDto>> getAll(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(savingsService.getAll(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsAccountResponseDto> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(savingsService.getById(currentUser, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SavingsAccountResponseDto> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestBody UpdateSavingsAccountRequestDto request
    ) {
        return ResponseEntity.ok(savingsService.update(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        savingsService.delete(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<SavingsAccountResponseDto> deposit(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DepositRequestDto request
    ) {
        return ResponseEntity.ok(savingsService.deposit(currentUser, id, request));
    }
}
