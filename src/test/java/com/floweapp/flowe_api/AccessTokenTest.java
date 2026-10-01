package com.floweapp.flowe_api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessTokenTest extends AuthTestSupport {
    @Test
    void at01_validCredentialsReturnTokens() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String accessToken = registerAndGetAccessToken(email, password);

        assertFalse(accessToken.isEmpty());

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void at02_missingAuthorizationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void at03_invalidAuthorizationFormatReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String accessToken = registerAndGetAccessToken(email, password);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void at04_accessTokenWithWrongSignatureReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String accessToken = registerAndGetAccessToken(email, password);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + changeJwtSignature(accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void at05_refreshTokenInsteadOfAccessReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        String refreshToken = registerAndGetRefreshToken(email, password);

        mockMvc.perform(get(PROTECTED_URL).header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }
}
