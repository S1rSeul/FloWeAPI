package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessTokenTest extends TestSupport {

    // ---------------------------------------------------------------
    // AT-01: Валидный access в Authorization: Bearer
    // ---------------------------------------------------------------
    @Test
    void at01_validCredentialsReturnTokens() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        assertFalse(accessToken.isEmpty());

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // AT-02: Нет заголовка Authorization
    // ---------------------------------------------------------------
    @Test
    void at02_missingAuthorizationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(GET_COUPLE_URL))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // AT-03: Некорректный формат заголовка
    // ---------------------------------------------------------------
    @Test
    void at03_invalidAuthorizationFormatReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", accessToken))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // AT-04: Неверная подпись access
    // ---------------------------------------------------------------
    @Test
    void at04_accessTokenWithWrongSignatureReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", "Bearer " + changeJwtSignature(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // AT-05: Refresh вместо access
    // ---------------------------------------------------------------
    @Test
    void at05_refreshTokenInsteadOfAccessReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String refreshToken = tokens.path("refreshToken").asString();

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }
}
