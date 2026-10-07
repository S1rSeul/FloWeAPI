package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RefreshRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class TestSupport extends IntegrationTestBase {
    protected String uniqueEmail() {
        return "test-email-" + UUID.randomUUID() + "@example.com";
    }

    protected String uniquePassword() {
        return "test-password-" + UUID.randomUUID();
    }

    protected String uniqueDisplayName() {
        return "test-displayName-" + UUID.randomUUID();
    }

    protected String uniqueCoupleName() {
        return "test-coupleName-" + UUID.randomUUID();
    }

    protected RegisterRequestDto uniqueRegisterRequestDto() {
        return new RegisterRequestDto(uniqueEmail(), uniquePassword(), uniqueDisplayName());
    }

    protected CoupleNameRequestDto uniqueCoupleNameRequestDto() {
        return new CoupleNameRequestDto(uniqueCoupleName());
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

    protected MvcResult register(RegisterRequestDto dto) throws Exception {
        return postJson(REGISTER_URL, dto);
    }

    protected MvcResult registerRaw(String jsonBody) throws Exception {
        return postRawJson(REGISTER_URL, jsonBody);
    }

    protected MvcResult login(LoginRequestDto dto) throws Exception {
        return postJson(LOGIN_URL, dto);
    }

    protected MvcResult refresh(String refreshToken) throws Exception {
        return postJson(REFRESH_URL, new RefreshRequestDto(refreshToken));
    }

    protected MvcResult logout(String refreshToken) throws Exception {
        return postJson(LOGOUT_URL, new RefreshRequestDto(refreshToken));
    }

    protected MvcResult registerSuccessfully(RegisterRequestDto dto) throws Exception {
        MvcResult result = register(dto);
        assertEquals(201, result.getResponse().getStatus(), "Подготовка: регистрация должна вернуть 201");
        return result;
    }

    protected String registerAndGetRefreshToken(String email, String password) throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(email, password, uniqueDisplayName());
        return token(registerSuccessfully(dto), "refreshToken");
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

    protected void assertRejected(MvcResult result, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, result.getResponse().getStatus(), "Получен неожиданный HTTP-статус");

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

    protected void assertRefreshSucceeded(MvcResult result) throws Exception {
        assertEquals(
                200,
                result.getResponse().getStatus(),
                "Успешный refresh должен вернуть 200"
        );

        token(result, "accessToken");
        token(result, "refreshToken");
    }

    protected void assertSuccessfulRegister(MvcResult result, RegisterRequestDto dto) throws Exception {
        JsonNode response = responseJson(result);

        assertAll("Ответ регистрации",
                () -> assertFalse(response.path("accessToken").asString().isBlank(), "Ответ должен содержать accessToken"),
                () -> assertFalse(response.path("refreshToken").asString().isBlank(), "Ответ должен содержать refreshToken"),
                () -> assertFalse(response.has("password"), "Ответ не должен содержать password"),
                () -> assertFalse(response.has("passwordHash"), "Ответ не должен содержать passwordHash")
        );

        User saved = userRepository.findByEmailIgnoreCase(dto.email()).orElseThrow(()
                -> new AssertionError("Пользователь должен быть создан в БД"));

        assertAll("Сохранённый пользователь",
                () -> assertEquals(dto.email(), saved.getEmail(), "Email должен сохраниться правильно"),
                () -> assertEquals(dto.displayName(), saved.getDisplayName(), "displayName должен сохраниться правильно"),
                () -> assertTrue(passwordEncoder.matches(dto.password(), saved.getPasswordHash()), "Хеш должен соответствовать исходному паролю")
        );
    }

    protected void assertSuccessfulLogin(MvcResult result, LoginRequestDto dto) throws Exception {
        JsonNode response = responseJson(result);

        assertAll("Ответ аутентификации",
                () -> assertFalse(response.path("accessToken").asString().isBlank(), "Ответ должен содержать accessToken"),
                () -> assertFalse(response.path("refreshToken").asString().isBlank(), "Ответ должен содержать refreshToken"),
                () -> assertFalse(response.has("password"), "Ответ не должен содержать password"),
                () -> assertFalse(response.has("passwordHash"), "Ответ не должен содержать passwordHash")
        );

        User saved = userRepository.findByEmailIgnoreCase(dto.email()).orElseThrow(()
                -> new AssertionError("Пользователь должен быть создан в БД"));

        assertAll("Сохранённый пользователь",
                () -> assertEquals(dto.email(), saved.getEmail(), "Email должен сохраниться правильно"),
                () -> assertTrue(passwordEncoder.matches(dto.password(), saved.getPasswordHash()), "Хеш должен соответствовать исходному паролю")
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
