package com.floweapp.flowe_api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RefreshTokenTest extends AuthTestSupport {
    // ---------------------------------------------------------------
    // RT-01: Валидный refresh
    // ---------------------------------------------------------------
    @Test
    void rt01_validRefreshReturnsNewAccessToken() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);

        MvcResult result = refresh(refreshToken);
        assertEquals(200, result.getResponse().getStatus());

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        String accessToken = response.path("accessToken").asString();
        assertFalse(accessToken.isBlank());

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // RT-02: Новый access имеет срок 15 минут
    // ---------------------------------------------------------------
    @Test
    void rt02_newAccessTokenLivesFifteenMinutes() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);
        MvcResult result = refresh(refreshToken);

        assertEquals(200, result.getResponse().getStatus());

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        assertEquals(900, response.path("expiresIn").asInt());

        String accessToken = response.path("accessToken").asString();
        assertFalse(accessToken.isBlank());

        String[] parts = accessToken.split("\\.");
        assertEquals(3, parts.length);

        JsonNode claims = objectMapper.readTree(
                Base64.getUrlDecoder().decode(parts[1])
        );

        assertEquals(900,
                claims.path("exp").asLong() - claims.path("iat").asLong()
        );
    }

    // ---------------------------------------------------------------
    // RT-03: Старый refresh остаётся валидным
    // ---------------------------------------------------------------
    @Test
    void rt03_oldRefreshRemainsValid() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);

        MvcResult firstRefresh = refresh(refreshToken);
        assertEquals(200, firstRefresh.getResponse().getStatus());
        accessTokenFrom(firstRefresh);

        MvcResult secondRefresh = refresh(refreshToken);
        assertEquals(200, secondRefresh.getResponse().getStatus());
        accessTokenFrom(secondRefresh);
    }

    // ---------------------------------------------------------------
    // RT-04: Повторный /refresh с тем же refresh
    // ---------------------------------------------------------------
    @Test
    void rt04_repeatedRefreshWithSameTokenReturnsNewAccess() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);

        MvcResult firstRefresh = refresh(refreshToken);
        MvcResult secondRefresh = refresh(refreshToken);

        assertEquals(200, firstRefresh.getResponse().getStatus());
        assertEquals(200, secondRefresh.getResponse().getStatus());

        String firstAccessToken = accessTokenFrom(firstRefresh);
        String secondAccessToken = accessTokenFrom(secondRefresh);

        assertNotEquals(firstAccessToken, secondAccessToken);
    }


    // ---------------------------------------------------------------
    // RT-05: Access отправлен на /refresh
    // ---------------------------------------------------------------
    @Test
    void rt05_accessTokenOnRefreshReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String accessToken = registerAndGetAccessToken(email, password);

        MvcResult result = refresh(accessToken);

        assertEquals(401, result.getResponse().getStatus());
    }

    // ---------------------------------------------------------------
    // RT-06: Refresh отсутствует в БД
    // ---------------------------------------------------------------
    @Test
    void rt06_refreshMissingInDatabaseReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = "missing-token";

        MvcResult result = refresh(refreshToken);

        assertEquals(401, result.getResponse().getStatus());
    }

    // ---------------------------------------------------------------
    // RT-07: Refresh после /logout
    // ---------------------------------------------------------------
    @Test
    void rt07_refreshAfterLogoutReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);

        MvcResult logoutResult = logout(refreshToken);
        assertEquals(204, logoutResult.getResponse().getStatus());

        MvcResult refreshResult = refresh(refreshToken);
        assertEquals(401, refreshResult.getResponse().getStatus());
    }

    // ---------------------------------------------------------------
    // RT-08: Refresh устройства A не влияет на refresh устройства B
    // ---------------------------------------------------------------
    @Test
    void rt08_refreshFromDeviceADoesNotAffectDeviceB() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshTokenA = registerAndGetRefreshToken(email, password);
        String refreshTokenB = loginAndGetRefreshToken(email, password);

        assertNotEquals(refreshTokenA, refreshTokenB);

        MvcResult logoutResult = logout(refreshTokenA);
        assertEquals(204, logoutResult.getResponse().getStatus());

        MvcResult deviceAResult = refresh(refreshTokenA);
        assertEquals(401, deviceAResult.getResponse().getStatus());

        MvcResult deviceBResult = refresh(refreshTokenB);
        assertEquals(200, deviceBResult.getResponse().getStatus());
        accessTokenFrom(deviceBResult);
    }
}
