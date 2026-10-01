package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.bouncycastle.cms.RecipientId.password;

public class LogoutTest extends AuthTestSupport {
    // ---------------------------------------------------------------
    // LO-01: Валидный refresh + access
    // ---------------------------------------------------------------
    @Test
    void lo01_logoutRevokesRefreshButNotAccess() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        JsonNode tokens = responseJson(resultRegister);
        String accessToken = tokens.path("accessToken").asString();
        String refreshToken = tokens.path("refreshToken").asString();

        assertFalse(accessToken.isBlank());
        assertFalse(refreshToken.isBlank());

        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(401, refresh(refreshToken).getResponse().getStatus());

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // LO-04: /logout без refresh
    // ---------------------------------------------------------------
    @Test
    void lo04_logoutWithoutRefreshReturnsBadRequest() throws Exception {
        mockMvc.perform(post(LOGOUT_URL).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // LO-05: /logout с невалидным refresh
    // ---------------------------------------------------------------
    @Test
    void lo05_logoutWithUnknownRefreshReturnsNoContent() throws Exception {
        MvcResult result = logout("not-issued-refresh-token");

        assertEquals(204, result.getResponse().getStatus());
    }

    // ---------------------------------------------------------------
    // LO-06: Повторный /logout с тем же refresh
    // ---------------------------------------------------------------
    @Test
    void lo06_repeatedLogoutReturnsNoContent() throws Exception {
        String refreshToken = registerAndGetRefreshToken(
                uniqueEmail(), uniquePassword()
        );

        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(204, logout(refreshToken).getResponse().getStatus());
        assertEquals(401, refresh(refreshToken).getResponse().getStatus());
    }

    // ---------------------------------------------------------------
    // LO-07: Logout с устройства A не удаляет refresh устройства B
    // ---------------------------------------------------------------
    @Test
    void lo07_logoutDeviceADoesNotRevokeDeviceB() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        String refreshA = responseJson(resultRegister).path("refreshToken").asString();

        LoginRequestDto loginRequestDto = new LoginRequestDto(registerRequestDtoDto.email(), registerRequestDtoDto.password());

        MvcResult resultLogin = login(loginRequestDto);
        assertSuccessfulLogin(resultLogin, loginRequestDto);

        String refreshB = responseJson(resultLogin).path("refreshToken").asString();

        assertFalse(refreshB.isBlank());
        assertNotEquals(refreshA, refreshB);

        assertEquals(204, logout(refreshA).getResponse().getStatus());
        assertEquals(401, refresh(refreshA).getResponse().getStatus());

        MvcResult deviceBResult = refresh(refreshB);
        assertEquals(200, deviceBResult.getResponse().getStatus());
        assertFalse(responseJson(deviceBResult).path("accessToken").asString().isBlank());
    }
}
