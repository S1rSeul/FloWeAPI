package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FloweApiApplicationTests extends IntegrationTestBase {
    private static final String REGISTER_URL = "/api/v1/auth/register";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // Базовые тесты
    // ---------------------------------------------------------------

    @Test
    void contextLoads() {
    }

    @Test
    void healthShouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    // ---------------------------------------------------------------
    // Генераторы данных
    // ---------------------------------------------------------------

    private String uniqueEmail() {
        return "test-email-" + UUID.randomUUID() + "@example.com";
    }

    private String uniquePassword() {
        return "test-password-" + UUID.randomUUID();
    }

    private String uniqueDisplayName() {
        return "test-displayName-" + UUID.randomUUID();
    }

    // ---------------------------------------------------------------
    // Служебные методы
    // ---------------------------------------------------------------

    private RegisterRequestDto validRequest(String email) {
        return new RegisterRequestDto(email, uniquePassword(), uniqueDisplayName());
    }

    private MvcResult register(RegisterRequestDto request) throws Exception {
        return registerRaw(objectMapper.writeValueAsString(request));
    }

    private MvcResult registerRaw(String jsonBody) throws Exception {
        return mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andReturn();
    }

    private void assertNoTokens(MvcResult result, String message) throws Exception {
        String body = result.getResponse().getContentAsString();
        if (!body.isBlank()) {
            JsonNode response = objectMapper.readTree(body);
            assertFalse(response.has("accessToken"), message + ": accessToken");
            assertFalse(response.has("refreshToken"), message + ": refreshToken");
        }
    }

    private void assertUserAbsent(String email) {
        assertFalse(
                userRepository.findByEmailIgnoreCase(email).isPresent(),
                "Пользователь с email '" + email + "' не должен сохраняться"
        );
    }

    private void assertRejected(MvcResult result, int expectedStatus, String email)
            throws Exception {
        assertEquals(expectedStatus, result.getResponse().getStatus(),
                "Неожиданный HTTP-статус при отклонении регистрации");
        assertNoTokens(result, "При ошибке регистрации токены не должны выдаваться");
        assertUserAbsent(email);
    }

    private void assertInvalidRegistration(MvcResult result, String email)
            throws Exception {
        assertRejected(result, 400, email);
    }

    private void assertSuccessfulRegistration(
            MvcResult result, String email, String password, String displayName
    ) throws Exception {
        assertEquals(201, result.getResponse().getStatus(),
                "Успешная регистрация должна возвращать 201 Created");

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        assertAll("Проверка токенов",
                () -> assertFalse(response.path("accessToken").asString().isBlank(),
                        "Ответ должен содержать accessToken"),
                () -> assertFalse(response.path("refreshToken").asString().isBlank(),
                        "Ответ должен содержать refreshToken")
        );

        User saved = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AssertionError(
                        "Пользователь должен быть сохранён в базе данных"));

        assertAll("Проверка сохранённого пользователя",
                () -> assertEquals(displayName, saved.getDisplayName(),
                        "В базе должен сохраниться правильный displayName"),
                () -> assertEquals(email, saved.getEmail(),
                        "В базе должен сохраниться правильный email"),
                () -> assertTrue(
                        passwordEncoder.matches(password, saved.getPasswordHash()),
                        "Хеш пароля должен соответствовать исходному паролю")
        );
    }

    // ---------------------------------------------------------------
    // R-01: валидный email + валидный пароль
    // ---------------------------------------------------------------
    @Test
    void r01_registerCreatesUserAndReturnsTokens() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        MvcResult result = register(new RegisterRequestDto(email, password, displayName));

        assertSuccessfulRegistration(result, email, password, displayName);
    }

    // ---------------------------------------------------------------
    // R-02: одинаковые пароли -> разные хеши (уникальная соль)
    // ---------------------------------------------------------------
    @Test
    void r02_samePasswordProducesDifferentHashes() throws Exception {
        String password = uniquePassword();
        String firstEmail = uniqueEmail();
        String secondEmail = uniqueEmail();

        MvcResult first = register(new RegisterRequestDto(firstEmail, password, uniqueDisplayName()));
        MvcResult second = register(new RegisterRequestDto(secondEmail, password, uniqueDisplayName()));

        assertEquals(201, first.getResponse().getStatus(), "Первая регистрация должна пройти");
        assertEquals(201, second.getResponse().getStatus(), "Вторая регистрация должна пройти");

        User firstUser = userRepository.findByEmailIgnoreCase(firstEmail).orElseThrow();
        User secondUser = userRepository.findByEmailIgnoreCase(secondEmail).orElseThrow();

        assertAll("Проверка хешей одинаковых паролей",
                () -> assertNotEquals(firstUser.getPasswordHash(), secondUser.getPasswordHash(),
                        "У одинаковых паролей хеши должны отличаться (уникальная соль)"),
                () -> assertNotEquals(password, firstUser.getPasswordHash(),
                        "Пароль не должен храниться в открытом виде"),
                () -> assertTrue(passwordEncoder.matches(password, firstUser.getPasswordHash()),
                        "Хеш первого пользователя должен соответствовать паролю"),
                () -> assertTrue(passwordEncoder.matches(password, secondUser.getPasswordHash()),
                        "Хеш второго пользователя должен соответствовать паролю")
        );
    }

    // ---------------------------------------------------------------
    // R-03: повторная регистрация на тот же email
    // ---------------------------------------------------------------
    @Test
    void r03_duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        String originalPassword = uniquePassword();

        MvcResult first = register(new RegisterRequestDto(email, originalPassword, uniqueDisplayName()));
        assertEquals(201, first.getResponse().getStatus(), "Первая регистрация должна пройти");
        String originalHash = userRepository.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash();

        MvcResult second = register(new RegisterRequestDto(email, uniquePassword(), uniqueDisplayName()));

        assertEquals(409, second.getResponse().getStatus(),
                "Повторная регистрация того же email должна возвращать 409 Conflict");
        assertNoTokens(second, "При дубликате токены не должны выдаваться");

        User stillSaved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(originalHash, stillSaved.getPasswordHash(),
                "Повторная регистрация не должна менять данные существующего пользователя");
    }

    // ---------------------------------------------------------------
    // R-04: тот же email в другом регистре
    // ---------------------------------------------------------------
    @Test
    void r04_duplicateEmailInDifferentCaseIsRejected() throws Exception {
        String lowerEmail = uniqueEmail().toLowerCase();
        String upperEmail = lowerEmail.toUpperCase();

        MvcResult first = register(validRequest(lowerEmail));
        assertEquals(201, first.getResponse().getStatus(), "Первая регистрация должна пройти");

        MvcResult second = register(validRequest(upperEmail));

        assertEquals(409, second.getResponse().getStatus(),
                "Email в другом регистре должен считаться дубликатом (409)");
        assertNoTokens(second, "При дубликате токены не должны выдаваться");
        assertTrue(userRepository.findByEmailIgnoreCase(lowerEmail).isPresent(),
                "Исходный пользователь должен остаться в базе");
    }

    // ---------------------------------------------------------------
    // R-05: email с пробелами по краям
    // ---------------------------------------------------------------
    @Test
    void r05_emailWithSurroundingSpacesIsTrimmed() throws Exception {
        String cleanEmail = uniqueEmail().toLowerCase();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        MvcResult result = register(new RegisterRequestDto("  " + cleanEmail + "  ", password, displayName));

        assertSuccessfulRegistration(result, cleanEmail, password, displayName);
    }

    // ---------------------------------------------------------------
    // R-06: пробел внутри email
    // ---------------------------------------------------------------
    @Test
    void r06_emailWithInnerSpaceIsRejected() throws Exception {
        String email = "us er-" + UUID.randomUUID() + "@mail.com";

        MvcResult result = register(validRequest(email));

        assertInvalidRegistration(result, email);
    }

    // ---------------------------------------------------------------
    // R-07: некорректные email
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "R-09: некорректный email ''{0}'' -> 400")
    @ValueSource(strings = {"user", "user@", "@mail.com", "user@mail", "user@.com"})
    void r07_malformedEmailIsRejected(String email) throws Exception {
        MvcResult result = register(validRequest(email));

        assertInvalidRegistration(result, email);
    }

    // ---------------------------------------------------------------
    // R-08: пустой email
    // ---------------------------------------------------------------
    @Test
    void r08_emptyEmailIsRejected() throws Exception {
        long usersBefore = userRepository.count();

        MvcResult result = register(validRequest(""));

        assertEquals(400, result.getResponse().getStatus(), "Пустой email должен возвращать 400");
        assertNoTokens(result, "При пустом email токены не должны выдаваться");
        assertEquals(usersBefore, userRepository.count(),
                "При пустом email количество пользователей не должно измениться");
    }

    // ---------------------------------------------------------------
    // R-09: пустой пароль
    // ---------------------------------------------------------------
    @Test
    void r09_emptyPasswordIsRejected() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(new RegisterRequestDto(email, "", uniqueDisplayName()));

        assertInvalidRegistration(result, email);
    }

    // ---------------------------------------------------------------
    // R-10: пустой displayName
    // ---------------------------------------------------------------
    @Test
    void r10_emptyDisplayNameIsRejected() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(new RegisterRequestDto(email, uniquePassword(), ""));

        assertInvalidRegistration(result, email);
    }

    // ---------------------------------------------------------------
    // R-11..R-14: границы длины пароля (min = 8, max = 72)
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "{0}: пароль длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-11, 7, 400",
            "R-12, 8, 201",
            "R-13, 72, 201",
            "R-14, 73, 400"
    })
    void passwordLengthBoundaries(String caseId, int length, int expectedStatus)
            throws Exception {
        String email = uniqueEmail();
        String password = "a".repeat(length);
        String displayName = uniqueDisplayName();

        MvcResult result = register(new RegisterRequestDto(email, password, displayName));

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для пароля длиной " + length);
        if (expectedStatus == 201) {
            assertSuccessfulRegistration(result, email, password, displayName);
        } else {
            assertInvalidRegistration(result, email);
        }
    }

    // ---------------------------------------------------------------
    // R-15..R-18: границы длины пароля (min = 8, max = 72)
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "{0}: displayName длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-15, 2, 400",
            "R-16, 3, 201",
            "R-17, 100, 201",
            "R-18, 101, 400"
    })
    void displayNameLengthBoundaries(String caseId, int length, int expectedStatus)
            throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = "a".repeat(length);

        MvcResult result = register(new RegisterRequestDto(email, password, displayName));

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + "Неожиданный статус для displayName длиной " + length);
        if (expectedStatus == 201) {
            assertSuccessfulRegistration(result, email, password, displayName);
        } else {
            assertInvalidRegistration(result, email);
        }
    }

    // ---------------------------------------------------------------
    // R-19: отсутствует Content-Type: application/json
    // ---------------------------------------------------------------
    @Test
    void r19_registerRejectsUnsupportedContentType() throws Exception {
        String email = uniqueEmail();
        RegisterRequestDto request = validRequest(email);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isUnsupportedMediaType());

        assertFalse(
                userRepository.findByEmailIgnoreCase(email).isPresent(),
                "Пользователь не должен создаваться при неподдерживаемом Content-Type"
        );
    }

    // ---------------------------------------------------------------
    // R-20: лишние поля в JSON
    // ---------------------------------------------------------------
    @Test
    void r20_unknownJsonFieldsAreIgnored() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        String body = """
            {
              "email": "%s",
              "password": "%s",
              "displayName": "%s",
              "role": "ADMIN",
              "id": "%s",
              "unknownField": "value"
            }
            """.formatted(email, password, displayName, UUID.randomUUID());

        MvcResult result = registerRaw(body);

        assertSuccessfulRegistration(result, email, password, displayName);
    }
}
