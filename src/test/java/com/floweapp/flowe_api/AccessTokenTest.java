package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Access token API")
public class AccessTokenTest extends TestSupport {

    @Test
    @DisplayName("AT-01: Валидный access-токен в Authorization Bearer пропускает запрос")
    void validAccessTokenAllowsRequest() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        assertFalse(accessToken.isBlank());

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AT-02: Запрос без Authorization отклоняется с HTTP 401")
    void at02_missingAuthorizationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(GET_COUPLE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AT-03: Authorization без Bearer отклоняется с HTTP 401")
    void at03_invalidAuthorizationFormatReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AT-04: Access-токен с неверной подписью отклоняется с HTTP 401")
    void at04_accessTokenWithWrongSignatureReturnsUnauthorized() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult resultRegister = register(dto);

        assertSuccessfulRegister(resultRegister, dto);
        JsonNode tokens = responseJson(resultRegister);

        String accessToken = tokens.path("accessToken").asString();

        mockMvc.perform(get(GET_COUPLE_URL).header("Authorization", "Bearer " + changeJwtSignature(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AT-05: Refresh-токен вместо access-токена отклоняется с HTTP 401")
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
