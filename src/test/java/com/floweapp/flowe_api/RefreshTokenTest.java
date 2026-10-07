package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Refresh token API")
class RefreshTokenTest extends AuthTestSupport {

    @Test
    @DisplayName("RT-01: Валидный refresh-токен возвращает новый access-токен")
    void validRefreshReturnsNewAccessToken() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String refreshToken = token(registration, "refreshToken");

        MvcResult refreshResult = refresh(refreshToken);

        assertRefreshSucceeded(refreshResult);

        String accessToken = accessTokenFrom(refreshResult);

        mockMvc.perform(get(PROTECTED_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RT-02: Новый access-токен действует 15 минут")
    void newAccessTokenLivesFifteenMinutes() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String refreshToken = token(registration, "refreshToken");
        MvcResult refreshResult = refresh(refreshToken);

        assertRefreshSucceeded(refreshResult);

        JsonNode response = responseJson(refreshResult);

        assertEquals(900, response.path("expiresIn").asInt());

        String accessToken = token(refreshResult, "accessToken");
        String[] parts = accessToken.split("\\.", -1);

        assertEquals(3, parts.length, "JWT должен состоять из трёх частей");

        JsonNode claims = objectMapper.readTree(
                Base64.getUrlDecoder().decode(parts[1])
        );

        assertEquals(
                900,
                claims.path("exp").asLong() - claims.path("iat").asLong(),
                "Срок действия access-токена должен составлять 900 секунд"
        );
    }

    @Test
    @DisplayName("RT-03: Исходный refresh-токен остаётся валидным после обновления")
    void oldRefreshRemainsValid() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String refreshToken = token(registration, "refreshToken");

        MvcResult firstRefresh = refresh(refreshToken);
        assertRefreshSucceeded(firstRefresh);

        MvcResult secondRefresh = refresh(refreshToken);
        assertRefreshSucceeded(secondRefresh);
    }

    @Test
    @DisplayName("RT-04: Повторный refresh с тем же токеном возвращает новый access-токен")
    void repeatedRefreshWithSameTokenReturnsNewAccess() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String refreshToken = token(registration, "refreshToken");

        MvcResult firstRefresh = refresh(refreshToken);
        MvcResult secondRefresh = refresh(refreshToken);

        assertRefreshSucceeded(firstRefresh);
        assertRefreshSucceeded(secondRefresh);

        String firstAccessToken = accessTokenFrom(firstRefresh);
        String secondAccessToken = accessTokenFrom(secondRefresh);

        assertNotEquals(
                firstAccessToken,
                secondAccessToken,
                "Повторное обновление должно выдавать новый access-токен"
        );
    }

    @Test
    @DisplayName("RT-05: Access-токен вместо refresh-токена отклоняется с HTTP 401")
    void accessTokenOnRefreshReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String accessToken = token(registration, "accessToken");

        assertEquals(401, refresh(accessToken).getResponse().getStatus());
    }

    @Test
    @DisplayName("RT-06: Refresh-токен, отсутствующий в базе данных, отклоняется с HTTP 401")
    void refreshMissingInDatabaseReturnsUnauthorized() throws Exception {
        assertEquals(
                401,
                refresh("missing-token").getResponse().getStatus()
        );
    }

    @Test
    @DisplayName("RT-07: Refresh-токен после Logout отклоняется с HTTP 401")
    void refreshAfterLogoutReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(dto);
        assertSuccessfulRegister(registration, dto);

        String refreshToken = token(registration, "refreshToken");

        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(401, refresh(refreshToken).getResponse().getStatus());
    }

    @Test
    @DisplayName("RT-08: Logout устройства A не отзывает refresh-токен устройства B")
    void refreshFromDeviceADoesNotAffectDeviceB() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        String refreshTokenA = token(registration, "refreshToken");

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email(),
                registrationDto.password()
        );

        MvcResult loginResult = login(loginDto);
        assertSuccessfulLogin(loginResult, loginDto);

        String refreshTokenB = token(loginResult, "refreshToken");

        assertNotEquals(
                refreshTokenA,
                refreshTokenB,
                "У разных устройств должны быть разные refresh-токены"
        );

        assertEquals(204, logout(refreshTokenA).getResponse().getStatus());
        assertEquals(401, refresh(refreshTokenA).getResponse().getStatus());

        MvcResult deviceBRefresh = refresh(refreshTokenB);

        assertEquals(200, deviceBRefresh.getResponse().getStatus());
        assertFalse(
                responseJson(deviceBRefresh)
                        .path("accessToken")
                        .asString()
                        .isBlank(),
                "Для устройства B должен быть выдан accessToken"
        );
    }
}