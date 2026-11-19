package com.floweapp.flowe_api.couple.controller;

import com.floweapp.flowe_api.couple.dto.CoupleMemberResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.dto.CoupleResponseDto;
import com.floweapp.flowe_api.couple.dto.JoinCoupleRequestDto;
import com.floweapp.flowe_api.couple.service.CoupleService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CoupleResponseDto create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
    ) {
        return coupleService.createCouple(currentUser, request);
    }

    @GetMapping("/me")
    public CoupleResponseDto getMyCouple(@AuthenticationPrincipal User currentUser) {
        return coupleService.getMyCouple(currentUser);
    }

    @PatchMapping("/me")
    public CoupleResponseDto updateMyCouple(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
    ) {
        return coupleService.updateMyCouple(currentUser, request);
    }

    @PostMapping("/join")
    public CoupleResponseDto join(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody JoinCoupleRequestDto request
    ) {
        return coupleService.joinCouple(currentUser, request);
    }

    @GetMapping("/me/members")
    public List<CoupleMemberResponseDto> getMembers(@AuthenticationPrincipal User currentUser) {
        return coupleService.getMembers(currentUser);
    }
}