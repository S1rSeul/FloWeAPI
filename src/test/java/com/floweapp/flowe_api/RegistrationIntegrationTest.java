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

class RegistrationIntegrationTest extends TestSupport {

    // ---------------------------------------------------------------
    // R-01: валидный email + валидный пароль
    // ---------------------------------------------------------------
    @Test
    void r01_registerCreatesUserAndReturnsTokens() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult result = registerSuccessfully(dto);

        assertSuccessfulRegister(result, dto);
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

        RegisterRequestDto dto1 = new RegisterRequestDto(firstEmail, password, displayName);
        RegisterRequestDto dto2 = new RegisterRequestDto(secondEmail, password, displayName);

        MvcResult result1 = registerSuccessfully(dto1);
        MvcResult result2 = registerSuccessfully(dto2);

        assertSuccessfulRegister(result1, dto1);
        assertSuccessfulRegister(result2, dto2);
    }

    // ---------------------------------------------------------------
    // R-03: повторная регистрация на тот же email
    // ---------------------------------------------------------------
    @Test
    void r03_duplicateEmailIsRejected() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult first = registerSuccessfully(dto);

        assertSuccessfulRegister(first, dto);

        User savedBefore = userRepository.findByEmailIgnoreCase(dto.email()).orElseThrow(()
                -> new AssertionError("Пользователь должен быть создан в БД"));

        MvcResult second = register(dto);
        assertRejected(second, 409);

        User savedAfter = userRepository.findByEmailIgnoreCase(dto.email()).orElseThrow(()
                -> new AssertionError("После отказа исходный пользователь должен остаться в БД"));

        assertAll("Повторная регистрация не меняет существующего пользователя",
                () -> assertEquals(savedBefore.getId(), savedAfter.getId(), "ID пользователя не должен измениться"),
                () -> assertEquals(savedBefore.getDisplayName(), savedAfter.getDisplayName(), "displayName не должен измениться"),
                () -> assertEquals(savedBefore.getPasswordHash(), savedAfter.getPasswordHash(), "Хеш пароля не должен измениться"),
                () -> assertTrue(passwordEncoder.matches(dto.password(), savedAfter.getPasswordHash()), "Исходный пароль должен оставаться действительным")
        );
    }

    // ---------------------------------------------------------------
    // R-04: тот же email в другом регистре
    // ---------------------------------------------------------------
    @Test
    void r04_duplicateEmailInDifferentCaseIsRejected() throws Exception {
        RegisterRequestDto dtoFirst = uniqueRegisterRequestDto();

        MvcResult first = registerSuccessfully(dtoFirst);

        assertSuccessfulRegister(first, dtoFirst);

        User savedBefore = userRepository.findByEmailIgnoreCase(dtoFirst.email()).orElseThrow(()
                -> new AssertionError("Пользователь должен быть создан в БД"));

        RegisterRequestDto dtoSecond = new RegisterRequestDto(dtoFirst.email().toUpperCase(), dtoFirst.password(), dtoFirst.displayName());
        MvcResult second = register(dtoSecond);
        assertRejected(second, 409);

        User savedAfter = userRepository.findByEmailIgnoreCase(dtoFirst.email()).orElseThrow(()
                -> new AssertionError("После отказа исходный пользователь должен остаться в БД"));

        assertAll("Регистрация на тот же email в другом регистре не меняет существующего пользователя",
                () -> assertEquals(savedBefore.getId(), savedAfter.getId(), "ID пользователя не должен измениться"),
                () -> assertEquals(savedBefore.getDisplayName(), savedAfter.getDisplayName(), "displayName не должен измениться"),
                () -> assertEquals(savedBefore.getPasswordHash(), savedAfter.getPasswordHash(), "Хеш пароля не должен измениться"),
                () -> assertTrue(passwordEncoder.matches(dtoFirst.password(), savedAfter.getPasswordHash()), "Исходный пароль должен оставаться действительным")
        );
    }

    // ---------------------------------------------------------------
    // R-05: email с пробелами по краям
    // ---------------------------------------------------------------
    @Test
    void r05_emailWithSurroundingSpacesIsTrimmed() throws Exception {
        String email = "  " + uniqueEmail() + "  ";

        RegisterRequestDto dto = new RegisterRequestDto(email, uniquePassword() , uniqueDisplayName());

        MvcResult result = registerSuccessfully(dto);

        assertSuccessfulRegister(result, dto);
    }

    // ---------------------------------------------------------------
    // R-06: пробел внутри email
    // ---------------------------------------------------------------
    @Test
    void r06_emailWithInnerSpaceIsRejected() throws Exception {
        String email = "us er-" + UUID.randomUUID() + "@mail.com";

        RegisterRequestDto dto = new RegisterRequestDto(email, uniquePassword() , uniqueDisplayName());

        MvcResult result = register(dto);
        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // R-07: некорректные email
    // ---------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = {"user", "user@", "@mail.com", "user@mail", "user@.com"})
    void r07_malformedEmailIsRejected(String email) throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(email, uniquePassword() , uniqueDisplayName());

        MvcResult result = register(dto);

        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // R-08: пустой email
    // ---------------------------------------------------------------
    @Test
    void r08_emptyEmailIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto("", uniquePassword() , uniqueDisplayName());

        MvcResult result = register(dto);

        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // R-09: пустой пароль
    // ---------------------------------------------------------------
    @Test
    void r09_emptyPasswordIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(uniqueEmail(), "" , uniqueDisplayName());

        MvcResult result = register(dto);

        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // R-10: пустой displayName
    // ---------------------------------------------------------------
    @Test
    void r10_emptyDisplayNameIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(uniqueEmail(), uniquePassword() , "");

        MvcResult result = register(dto);

        assertRejected(result, 400);
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
        RegisterRequestDto dto = new RegisterRequestDto(uniqueEmail(), "a".repeat(length), uniqueDisplayName());

        MvcResult result = register(dto);

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для пароля длиной " + length);

        if (expectedStatus == 201) {
            assertSuccessfulRegister(result, dto);
        } else {
            assertRejected(result, expectedStatus);
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
        RegisterRequestDto dto = new RegisterRequestDto(uniqueEmail(), uniquePassword(), "a".repeat(length));

        MvcResult result = register(dto);

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для displayName длиной " + length);

        if (expectedStatus == 201) {
            assertSuccessfulRegister(result, dto);
        } else {
            assertRejected(result, expectedStatus);
        }
    }

    // ---------------------------------------------------------------
    // R-19: отсутствует Content-Type: application/json
    // ---------------------------------------------------------------
    @Test
    void r19_missingContentTypeIsRejected() throws Exception {
        String email = uniqueEmail();
        String body = objectMapper.writeValueAsString(new RegisterRequestDto(email, uniquePassword(), uniqueDisplayName()));

        MvcResult result = mockMvc.perform(post(REGISTER_URL).content(body)).andReturn();

        assertRejected(result, 415);
    }

    // ---------------------------------------------------------------
    // R-20: лишние поля в JSON
    // ---------------------------------------------------------------
    @Test
    void r20_unknownJsonFieldsAreIgnored() throws Exception {
        UUID injectedId = UUID.randomUUID();

        RegisterRequestDto dto = uniqueRegisterRequestDto();

        String body = """
            {
              "id": "%s",
              "email": "%s",
              "password": "%s",
              "displayName": "%s",
              "unknownField": "value"
            }
            """.formatted(UUID.randomUUID(), dto.email(), dto.password(), dto.displayName());

        MvcResult result = registerRaw(body);

        assertSuccessfulRegister(result, dto);

        User saved = userRepository.findByEmailIgnoreCase(dto.email()).orElseThrow();
        assertNotEquals(injectedId, saved.getId(), "Лишнее поле id из запроса не должно попадать в сущность");
    }
}