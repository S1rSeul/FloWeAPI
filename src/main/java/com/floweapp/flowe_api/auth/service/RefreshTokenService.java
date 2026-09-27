package com.floweapp.flowe_api.auth.service;

import com.floweapp.flowe_api.auth.entity.RefreshToken;
import com.floweapp.flowe_api.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Transactional
    public void store(UUID userId, String rawToken, OffsetDateTime expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(expiresAt)
                .createdAt(OffsetDateTime.now())
                .build();
        repository.save(token);
    }

    public Optional<RefreshToken> findByRawToken(String rawToken) {
        return repository.findByTokenHash(hash(rawToken));
    }

    @Transactional
    public int deleteByRawToken(String rawToken) {
        return repository.deleteByTokenHash(hash(rawToken));
    }

    @Transactional
    public int deleteExpired() {
        return repository.deleteExpiredBefore(OffsetDateTime.now());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
