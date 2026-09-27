package com.floweapp.flowe_api.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        int deleted = refreshTokenService.deleteExpired();
        if (deleted > 0) {
            log.info("Очистка refresh токенов: удалено {} просроченных токенов", deleted);
        }
    }
}
