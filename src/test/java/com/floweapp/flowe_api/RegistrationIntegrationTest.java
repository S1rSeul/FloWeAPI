package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class RegistrationIntegrationTest extends AuthTestSupport {

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
        String firstEmail = uniqueEmail();
        String secondEmail = uniqueEmail();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        registerSuccessfully(firstEmail, password, displayName);
        registerSuccessfully(secondEmail, password, displayName);

        User first = userRepository.findByEmailIgnoreCase(firstEmail).orElseThrow();
        User second = userRepository.findByEmailIgnoreCase(secondEmail).orElseThrow();

        assertAll("Хеши одинаковых паролей",
                () -> assertNotEquals(first.getPasswordHash(), second.getPasswordHash(), "Одинаковые пароли должны иметь разные хеши"),
                () -> assertNotEquals(password, first.getPasswordHash(), "Пароль не должен храниться в открытом виде"),
                () -> assertTrue(passwordEncoder.matches(password, first.getPasswordHash()), "Хеш первого пользователя должен соответствовать паролю"),
                () -> assertTrue(passwordEncoder.matches(password, second.getPasswordHash()), "Хеш второго пользователя должен соответствовать паролю")
        );
    }

    // ---------------------------------------------------------------
    // R-03: повторная регистрация на тот же email
    // ---------------------------------------------------------------
    @Test
    void r03_duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        String firstPassword = uniquePassword();
        String firstDisplayName = uniqueDisplayName();

        registerSuccessfully(email, firstPassword, firstDisplayName);

        User originalUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        UUID originalId = originalUser.getId();
        String originalHash = originalUser.getPasswordHash();

        MvcResult second = register(validRequest(email));

        assertRejected(second, 409);

        User savedAfterSecondRequest = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AssertionError("После отказа исходный пользователь должен остаться в БД"));

        assertAll(
                "Повторная регистрация не меняет существующего пользователя",
                () -> assertEquals(originalId, savedAfterSecondRequest.getId(), "ID пользователя не должен измениться"),
                () -> assertEquals(firstDisplayName, savedAfterSecondRequest.getDisplayName(), "displayName не должен измениться"),
                () -> assertEquals(originalHash, savedAfterSecondRequest.getPasswordHash(), "Хеш пароля не должен измениться"),
                () -> assertTrue(passwordEncoder.matches(firstPassword, savedAfterSecondRequest.getPasswordHash()), "Исходный пароль должен оставаться действительным")
        );
    }

    // ---------------------------------------------------------------
    // R-04: тот же email в другом регистре
    // ---------------------------------------------------------------
    @Test
    void r04_duplicateEmailInDifferentCaseIsRejected() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        registerSuccessfully(email, password, displayName);

        MvcResult second = registerRawFields(email.toUpperCase(), password, displayName);

        assertRejected(second, 409);

        User stillSaved = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        assertTrue(passwordEncoder.matches(password, stillSaved.getPasswordHash()), "Исходный пользователь не должен измениться");
    }

    // ---------------------------------------------------------------
    // R-05: email с пробелами по краям
    // ---------------------------------------------------------------
    @Test
    void r05_emailWithSurroundingSpacesIsTrimmed() throws Exception {
        String cleanEmail = uniqueEmail();
        String emailWithSpaces = "  " + cleanEmail + "  ";
        String password = uniquePassword();
        String displayName = uniqueDisplayName();

        MvcResult result = registerRawFields(emailWithSpaces, password, displayName);

        assertSuccessfulRegistration(result, cleanEmail, password, displayName);
    }

    // ---------------------------------------------------------------
    // R-06: пробел внутри email
    // ---------------------------------------------------------------
    @Test
    void r06_emailWithInnerSpaceIsRejected() throws Exception {
        String email = "us er-" + UUID.randomUUID() + "@mail.com";

        MvcResult result = register(validRequest(email));

        assertRegistrationRejected(result, 400, email);
    }

    // ---------------------------------------------------------------
    // R-07: некорректные email
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = {"user", "user@", "@mail.com", "user@mail", "user@.com"})
    void r07_malformedEmailIsRejected(String email) throws Exception {
        MvcResult result = register(validRequest(email));

        assertRegistrationRejected(result, 400, email);
    }

    // ---------------------------------------------------------------
    // R-08: пустой email
    // ---------------------------------------------------------------
    @Test
    void r08_emptyEmailIsRejected() throws Exception {
        long usersBefore = userRepository.count();

        MvcResult result = register(validRequest(""));

        assertRejected(result, 400);
        assertEquals(usersBefore, userRepository.count(), "При пустом email количество пользователей не должно измениться");
    }

    // ---------------------------------------------------------------
    // R-09: пустой пароль
    // ---------------------------------------------------------------
    @Test
    void r09_emptyPasswordIsRejected() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(new RegisterRequestDto(email, "", uniqueDisplayName()));

        assertRegistrationRejected(result, 400, email);
    }

    // ---------------------------------------------------------------
    // R-10: пустой displayName
    // ---------------------------------------------------------------
    @Test
    void r10_emptyDisplayNameIsRejected() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(new RegisterRequestDto(email, uniquePassword(), ""));

        assertRegistrationRejected(result, 400, email);
    }

    // ---------------------------------------------------------------
    // R-11..R-14: границы длины пароля (min = 8, max = 72)
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "{0}: password длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-11, 7, 400",
            "R-12, 8, 201",
            "R-13, 72, 201",
            "R-14, 73, 400"
    })
    void r11ToR14_passwordLengthBoundaries(String caseId, int length, int expectedStatus) throws Exception {
        String email = uniqueEmail();
        String password = "a".repeat(length);
        String displayName = uniqueDisplayName();

        MvcResult result = register(new RegisterRequestDto(email, password, displayName));

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для пароля длиной " + length);

        if (expectedStatus == 201) {
            assertSuccessfulRegistration(result, email, password, displayName);
        } else {
            assertRegistrationRejected(result, expectedStatus, email);
        }
    }

    // ---------------------------------------------------------------
    // R-15..R-18: границы длины displayName (min = 3, max = 100)
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "{0}: displayName длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-15, 2, 400",
            "R-16, 3, 201",
            "R-17, 100, 201",
            "R-18, 101, 400"
    })
    void r15ToR18_displayNameLengthBoundaries(String caseId, int length, int expectedStatus) throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = "a".repeat(length);

        MvcResult result = register(new RegisterRequestDto(email, password, displayName));

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для displayName длиной " + length);

        if (expectedStatus == 201) {
            assertSuccessfulRegistration(result, email, password, displayName);
        } else {
            assertRegistrationRejected(result, expectedStatus, email);
        }
    }

    // ---------------------------------------------------------------
    // R-19: отсутствует Content-Type: application/json
    // ---------------------------------------------------------------
    @Test
    void r19_missingContentTypeIsRejected() throws Exception {
        String email = uniqueEmail();
        String body = objectMapper.writeValueAsString(validRequest(email));

        MvcResult result = mockMvc.perform(post(REGISTER_URL).content(body)).andReturn();

        assertRegistrationRejected(result, 415, email);
    }

    // ---------------------------------------------------------------
    // R-20: лишние поля в JSON
    // ---------------------------------------------------------------
    @Test
    void r20_unknownJsonFieldsAreIgnored() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();
        String displayName = uniqueDisplayName();
        UUID injectedId = UUID.randomUUID();

        String body = """
            {
              "email": "%s",
              "password": "%s",
              "displayName": "%s",
              "id": "%s",
              "role": "ADMIN",
              "unknownField": "value"
            }
            """.formatted(email, password, displayName, injectedId);

        MvcResult result = registerRaw(body);

        assertSuccessfulRegistration(result, email, password, displayName);

        User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotEquals(injectedId, saved.getId(),
                "Лишнее поле id из запроса не должно попадать в сущность");
    }
}