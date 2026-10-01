package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RefreshRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class AuthTestSupport extends IntegrationTestBase {

    protected static final String REGISTER_URL = "/api/v1/auth/register";
    protected static final String LOGIN_URL = "/api/v1/auth/login";
    protected static final String REFRESH_URL = "/api/v1/auth/refresh";
    protected static final String LOGOUT_URL = "/api/v1/auth/logout";
    protected static final String PROTECTED_URL = "/api/v1/couples/me";

    protected String uniqueEmail() {
        return "test-email-" + UUID.randomUUID() + "@example.com";
    }

    protected String uniquePassword() {
        return "test-password-" + UUID.randomUUID();
    }

    protected String uniqueDisplayName() {
        return "test-displayName-" + UUID.randomUUID();
    }

    protected RegisterRequestDto validRequest(String email) {
        return new RegisterRequestDto(email, uniquePassword(), uniqueDisplayName());
    }

    protected MvcResult postJson(String url, Object body) throws Exception {
        return postRawJson(url, objectMapper.writeValueAsString(body));
    }

    protected MvcResult postRawJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    protected MvcResult register(RegisterRequestDto request) throws Exception {
        return postJson(REGISTER_URL, request);
    }

    protected MvcResult registerRaw(String jsonBody) throws Exception {
        return postRawJson(REGISTER_URL, jsonBody);
    }

    protected MvcResult registerRawFields(String email, String password, String displayName) throws Exception {
        return postJson(REGISTER_URL, Map.of(
                "email", email,
                "password", password,
                "displayName", displayName
        ));
    }

    protected MvcResult login(String email, String password) throws Exception {
        return postJson(LOGIN_URL, new LoginRequestDto(email, password));
    }

    protected MvcResult loginRawFields(String email, String password) throws Exception {
        String json = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

        return postRawJson(LOGIN_URL, json);
    }

    protected MvcResult refresh(String refreshToken) throws Exception {
        return postJson(REFRESH_URL, new RefreshRequestDto(refreshToken));
    }

    protected MvcResult logout(String refreshToken) throws Exception {
        return postJson(LOGOUT_URL, new RefreshRequestDto(refreshToken));
    }

    protected MvcResult registerSuccessfully(String email, String password, String displayName) throws Exception {
        MvcResult result = register(new RegisterRequestDto(
                email,
                password,
                displayName
        ));

        assertEquals(
                201,
                result.getResponse().getStatus(),
                "Подготовка: регистрация должна вернуть 201"
        );

        return result;
    }

    protected MvcResult registerSuccessfully(String email, String password) throws Exception {
        return registerSuccessfully(email, password, uniqueDisplayName());
    }

    protected MvcResult loginSuccessfully(String email, String password) throws Exception {
        MvcResult result = login(email, password);

        assertEquals(
                200,
                result.getResponse().getStatus(),
                "Подготовка: login должен вернуть 200"
        );

        return result;
    }

    protected String registerAndGetRefreshToken(String email, String password) throws Exception {
        return token(registerSuccessfully(email, password), "refreshToken");
    }

    protected String registerAndGetAccessToken(String email, String password) throws Exception {
        return token(registerSuccessfully(email, password), "accessToken");
    }

    protected String loginAndGetAccessToken(String email, String password) throws Exception {
        return token(loginSuccessfully(email, password), "accessToken");
    }

    protected String loginAndGetRefreshToken(String email, String password) throws Exception {
        return token(loginSuccessfully(email, password), "refreshToken");
    }

    protected JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
    }

    protected String token(MvcResult result, String field) throws Exception {
        String value = responseJson(result).path(field).asString();

        assertFalse(
                value.isBlank(),
                "Ответ должен содержать непустой " + field
        );

        return value;
    }

    protected void assertNoTokens(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();

        if (body.isBlank()) {
            return;
        }

        JsonNode response = objectMapper.readTree(body);

        assertFalse(
                response.has("accessToken"),
                "При ошибке accessToken не должен выдаваться"
        );
        assertFalse(
                response.has("refreshToken"),
                "При ошибке refreshToken не должен выдаваться"
        );
    }

    protected void assertRejected(MvcResult result, int expectedStatus) throws Exception {
        assertEquals(
                expectedStatus,
                result.getResponse().getStatus(),
                "Получен неожиданный HTTP-статус"
        );

        assertNoTokens(result);
    }

    protected void assertLoginRejected(MvcResult result, int expectedStatus) throws Exception {
        assertRejected(result, expectedStatus);
    }

    protected void assertRegistrationRejected(MvcResult result, int expectedStatus, String email) throws Exception {
        assertRejected(result, expectedStatus);

        assertFalse(
                userRepository.findByEmailIgnoreCase(email).isPresent(),
                "Пользователь не должен быть сохранён: " + email
        );
    }

    protected void assertLoginSucceeded(MvcResult result) throws Exception {
        assertEquals(
                200,
                result.getResponse().getStatus(),
                "Успешный вход должен вернуть 200"
        );

        token(result, "accessToken");
        token(result, "refreshToken");
    }

    protected void assertRefreshSucceeded(MvcResult result) throws Exception {
        assertEquals(
                200,
                result.getResponse().getStatus(),
                "Успешный refresh должен вернуть 200"
        );

        token(result, "accessToken");
        token(result, "refreshToken");
    }

    protected void assertSuccessfulRegistration(MvcResult result, String email, String password, String displayName) throws Exception {
        assertEquals(
                201,
                result.getResponse().getStatus(),
                "Регистрация должна вернуть 201 Created"
        );

        JsonNode response = responseJson(result);

        assertAll(
                "Ответ регистрации",
                () -> assertFalse(
                        response.path("accessToken").asString().isBlank(),
                        "Ответ должен содержать accessToken"
                ),
                () -> assertFalse(
                        response.path("refreshToken").asString().isBlank(),
                        "Ответ должен содержать refreshToken"
                ),
                () -> assertFalse(
                        response.has("password"),
                        "Ответ не должен содержать password"
                ),
                () -> assertFalse(
                        response.has("passwordHash"),
                        "Ответ не должен содержать passwordHash"
                )
        );

        User saved = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AssertionError(
                        "Пользователь должен быть создан в БД"
                ));

        assertAll(
                "Сохранённый пользователь",
                () -> assertEquals(
                        email,
                        saved.getEmail(),
                        "Email должен сохраниться правильно"
                ),
                () -> assertEquals(
                        displayName,
                        saved.getDisplayName(),
                        "displayName должен сохраниться правильно"
                ),
                () -> assertTrue(
                        passwordEncoder.matches(password, saved.getPasswordHash()),
                        "Хеш должен соответствовать исходному паролю"
                )
        );
    }

    protected String changeJwtSignature(String token) {
        String[] parts = token.split("\\.", -1);
        assertEquals(3, parts.length);

        String signature = parts[2];
        assertFalse(signature.isEmpty());

        char replacement = signature.charAt(0) == 'A' ? 'B' : 'A';
        parts[2] = replacement + signature.substring(1);

        return String.join(".", parts);
    }

    protected String accessTokenFrom(MvcResult result) throws Exception {
        String accessToken = responseJson(result)
                .path("accessToken")
                .asString();

        assertFalse(accessToken.isBlank());
        return accessToken;
    }
}
