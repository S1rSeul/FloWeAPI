package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Logout API")
public class LogoutTest extends TestSupport {
    @Test
    @DisplayName("LO-01: Logout отзывает refresh-токен, но access-токен продолжает работать")
    void logoutRevokesRefreshButNotAccess() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        JsonNode tokens = responseJson(registration);
        String accessToken = tokens.path("accessToken").asString();
        String refreshToken = tokens.path("refreshToken").asString();

        assertFalse(accessToken.isBlank(), "Регистрация должна вернуть accessToken");
        assertFalse(refreshToken.isBlank(), "Регистрация должна вернуть refreshToken");

        assertEquals(204, logout(refreshToken).getResponse().getStatus());

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        assertEquals(401, refresh(refreshToken).getResponse().getStatus());
    }

    @Test
    @DisplayName("LO-04: Logout без refresh-токена возвращает HTTP 400")
    void logoutWithoutRefreshReturnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("LO-05: Logout с неизвестным refresh-токеном возвращает HTTP 204")
    void logoutWithUnknownRefreshReturnsNoContent() throws Exception {
        assertEquals(204, logout("not-issued-refresh-token").getResponse().getStatus());
    }

    @Test
    @DisplayName("LO-06: Повторный Logout с тем же refresh-токеном безопасен")
    void repeatedLogoutReturnsNoContent() throws Exception {
        String refreshToken = registerAndGetRefreshToken(
                uniqueEmail(),
                uniquePassword()
        );

        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(401, refresh(refreshToken).getResponse().getStatus());
    }

    @Test
    @DisplayName("LO-07: Logout на устройстве A не отзывает refresh-токен устройства B")
    void logoutDeviceADoesNotRevokeDeviceB() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        String refreshTokenA = token(registration, "refreshToken");

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email(),
                registrationDto.password()
        );

        MvcResult secondLogin = login(loginDto);
        assertSuccessfulLogin(secondLogin, loginDto);

        String refreshTokenB = token(secondLogin, "refreshToken");

        assertNotEquals(
                refreshTokenA,
                refreshTokenB,
                "У разных устройств должны быть разные refresh-токены"
        );

        assertEquals(204, logout(refreshTokenA).getResponse().getStatus());
        assertEquals(401, refresh(refreshTokenA).getResponse().getStatus());

        MvcResult secondDeviceRefresh = refresh(refreshTokenB);

        assertEquals(200, secondDeviceRefresh.getResponse().getStatus());
        assertFalse(
                responseJson(secondDeviceRefresh)
                        .path("accessToken")
                        .asString()
                        .isBlank(),
                "Для второго устройства должен быть выдан accessToken"
        );
    }
}