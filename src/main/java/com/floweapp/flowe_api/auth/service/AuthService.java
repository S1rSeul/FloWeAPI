package com.floweapp.flowe_api.auth.service;

import com.floweapp.flowe_api.auth.dto.AuthResponseDto;
import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RefreshRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.auth.exception.EmailAlreadyExistsException;
import com.floweapp.flowe_api.auth.exception.InvalidCredentialsException;
import com.floweapp.flowe_api.auth.exception.InvalidTokenException;
import com.floweapp.flowe_api.config.JwtProperties;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = request.email();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .createdAt(OffsetDateTime.now())
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user);
    }

    public AuthResponseDto refresh(RefreshRequestDto request) {
        String rawRefresh = request.refreshToken();

        var stored = refreshTokenService.findByRawToken(rawRefresh)
                .orElseThrow(() -> new InvalidTokenException("Refresh токен не найден"));

        if (stored.isExpired()) {
            throw new InvalidTokenException("Refresh токен просрочен");
        }

        String email;
        try {
            email = jwtService.extractUsername(rawRefresh);
        } catch (Exception e) {
            throw new InvalidTokenException("Неверный refresh токен");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(InvalidCredentialsException::new);

        if (!jwtService.isTokenValid(rawRefresh, user)) {
            throw new InvalidTokenException("Неверный refresh токен");
        }

        String newAccess = jwtService.generateAccessToken(user);
        return new AuthResponseDto(newAccess, rawRefresh, jwtProperties.accessTokenExpiration() / 1000);
    }

    @Transactional
    public void logout(RefreshRequestDto request) {
        refreshTokenService.deleteByRawToken(request.refreshToken());
    }

    private AuthResponseDto issueTokens(UserDetails user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        OffsetDateTime refreshExpiresAt = jwtService.extractExpiration(refreshToken);
        refreshTokenService.store(((User) user).getId(), refreshToken, refreshExpiresAt);

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpiration() / 1000
        );
    }
}
