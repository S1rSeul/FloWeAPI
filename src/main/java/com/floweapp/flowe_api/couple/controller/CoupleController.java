package com.floweapp.flowe_api.couple.controller;

import com.floweapp.flowe_api.couple.dto.CoupleResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.service.CoupleService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @PostMapping
    public ResponseEntity<CoupleResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
            ) {
        CoupleResponseDto response = coupleService.createCouple(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CoupleResponseDto> getMyCouple(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(coupleService.getMyCouple(currentUser));
    }

    @PatchMapping("/me")
    public ResponseEntity<CoupleResponseDto> updateMyCouple(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
    ) {
        return ResponseEntity.ok(coupleService.updateMyCouple(currentUser, request));
    }
}
