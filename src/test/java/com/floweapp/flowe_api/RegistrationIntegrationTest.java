package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Registration API")
class RegistrationIntegrationTest extends AuthTestSupport {

    @Test
    @DisplayName("R-01: Валидные данные создают пользователя и возвращают токены")
    void validRegistrationCreatesUserAndReturnsTokens() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult result = registerSuccessfully(dto);

        assertSuccessfulRegister(result, dto);
    }

    @Test
    @DisplayName("R-02: Одинаковые пароли получают разные хеши")
    void samePasswordProducesDifferentHashes() throws Exception {
        String password = uniquePassword();

        RegisterRequestDto firstDto = new RegisterRequestDto(
                uniqueEmail(),
                password,
                uniqueDisplayName()
        );
        RegisterRequestDto secondDto = new RegisterRequestDto(
                uniqueEmail(),
                password,
                uniqueDisplayName()
        );

        MvcResult firstResult = registerSuccessfully(firstDto);
        MvcResult secondResult = registerSuccessfully(secondDto);

        assertSuccessfulRegister(firstResult, firstDto);
        assertSuccessfulRegister(secondResult, secondDto);

        User firstUser = userRepository.findByEmailIgnoreCase(firstDto.email())
                .orElseThrow();
        User secondUser = userRepository.findByEmailIgnoreCase(secondDto.email())
                .orElseThrow();

        assertNotEquals(
                firstUser.getPasswordHash(),
                secondUser.getPasswordHash(),
                "Одинаковые пароли должны иметь разные хеши"
        );
    }

    @Test
    @DisplayName("R-03: Повторная регистрация на тот же email отклоняется")
    void duplicateEmailIsRejected() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();

        MvcResult firstResult = registerSuccessfully(dto);
        assertSuccessfulRegister(firstResult, dto);

        User savedBefore = userRepository.findByEmailIgnoreCase(dto.email())
                .orElseThrow(() -> new AssertionError(
                        "Пользователь должен быть создан в БД"
                ));

        MvcResult secondResult = register(dto);

        assertRejected(secondResult, 409);

        User savedAfter = userRepository.findByEmailIgnoreCase(dto.email())
                .orElseThrow(() -> new AssertionError(
                        "После отказа исходный пользователь должен остаться в БД"
                ));

        assertAll(
                "Повторная регистрация не меняет существующего пользователя",
                () -> assertEquals(
                        savedBefore.getId(),
                        savedAfter.getId(),
                        "ID пользователя не должен измениться"
                ),
                () -> assertEquals(
                        savedBefore.getDisplayName(),
                        savedAfter.getDisplayName(),
                        "displayName не должен измениться"
                ),
                () -> assertEquals(
                        savedBefore.getPasswordHash(),
                        savedAfter.getPasswordHash(),
                        "Хеш пароля не должен измениться"
                ),
                () -> assertTrue(
                        passwordEncoder.matches(
                                dto.password(),
                                savedAfter.getPasswordHash()
                        ),
                        "Исходный пароль должен оставаться действительным"
                )
        );
    }

    @Test
    @DisplayName("R-04: Повторная регистрация на email в другом регистре отклоняется")
    void duplicateEmailInDifferentCaseIsRejected() throws Exception {
        RegisterRequestDto firstDto = uniqueRegisterRequestDto();

        MvcResult firstResult = registerSuccessfully(firstDto);
        assertSuccessfulRegister(firstResult, firstDto);

        User savedBefore = userRepository.findByEmailIgnoreCase(firstDto.email())
                .orElseThrow(() -> new AssertionError(
                        "Пользователь должен быть создан в БД"
                ));

        RegisterRequestDto secondDto = new RegisterRequestDto(
                firstDto.email().toUpperCase(),
                firstDto.password(),
                firstDto.displayName()
        );

        MvcResult secondResult = register(secondDto);

        assertRejected(secondResult, 409);

        User savedAfter = userRepository.findByEmailIgnoreCase(firstDto.email())
                .orElseThrow(() -> new AssertionError(
                        "После отказа исходный пользователь должен остаться в БД"
                ));

        assertAll(
                "Регистрация на email в другом регистре не меняет пользователя",
                () -> assertEquals(
                        savedBefore.getId(),
                        savedAfter.getId(),
                        "ID пользователя не должен измениться"
                ),
                () -> assertEquals(
                        savedBefore.getDisplayName(),
                        savedAfter.getDisplayName(),
                        "displayName не должен измениться"
                ),
                () -> assertEquals(
                        savedBefore.getPasswordHash(),
                        savedAfter.getPasswordHash(),
                        "Хеш пароля не должен измениться"
                ),
                () -> assertTrue(
                        passwordEncoder.matches(
                                firstDto.password(),
                                savedAfter.getPasswordHash()
                        ),
                        "Исходный пароль должен оставаться действительным"
                )
        );
    }

    @Test
    @DisplayName("R-05: Пробелы по краям email обрабатываются корректно")
    void emailWithSurroundingSpacesIsHandled() throws Exception {
        String email = " " + uniqueEmail() + " ";
        String normalizedEmail = email.trim();

        RegisterRequestDto dto = new RegisterRequestDto(
                email,
                uniquePassword(),
                uniqueDisplayName()
        );

        MvcResult result = register(dto);

        assertSuccessfulRegister(result, dto);

        User saved = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AssertionError(
                        "Пользователь с обработанным email должен быть создан"
                ));

        assertEquals(normalizedEmail, saved.getEmail());
    }

    @Test
    @DisplayName("R-06: Пробел внутри email отклоняется")
    void emailWithInnerSpaceIsRejected() throws Exception {
        String email = "us er-" + UUID.randomUUID() + "@mail.com";

        RegisterRequestDto dto = new RegisterRequestDto(
                email,
                uniquePassword(),
                uniqueDisplayName()
        );

        MvcResult result = register(dto);

        assertRejected(result, 400);
    }

    @DisplayName("Некорректный формат email отклоняется")
    @ParameterizedTest(name = "R-07: email \"{0}\" -> HTTP 400")
    @ValueSource(strings = {
            "user",
            "user@",
            "@mail.com",
            "user@mail",
            "user@.com"
    })
    void malformedEmailIsRejected(String email) throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                email,
                uniquePassword(),
                uniqueDisplayName()
        );

        MvcResult result = register(dto);

        assertRejected(result, 400);
    }

    @Test
    @DisplayName("R-08: Пустой email отклоняется")
    void emptyEmailIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                "",
                uniquePassword(),
                uniqueDisplayName()
        );

        assertRejected(register(dto), 400);
    }

    @Test
    @DisplayName("R-09: Пустой пароль отклоняется")
    void emptyPasswordIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                uniqueEmail(),
                "",
                uniqueDisplayName()
        );

        assertRejected(register(dto), 400);
    }

    @Test
    @DisplayName("R-10: Пустой displayName отклоняется")
    void emptyDisplayNameIsRejected() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                uniqueEmail(),
                uniquePassword(),
                ""
        );

        assertRejected(register(dto), 400);
    }

    @DisplayName("Граничные значения длины пароля")
    @ParameterizedTest(name = "{0}: пароль длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-11, 7, 400",
            "R-12, 8, 201",
            "R-13, 72, 201",
            "R-14, 73, 400"
    })
    void passwordLengthBoundary(
            String caseId,
            int length,
            int expectedStatus
    ) throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                uniqueEmail(),
                "a".repeat(length),
                uniqueDisplayName()
        );

        MvcResult result = register(dto);

        assertEquals(
                expectedStatus,
                result.getResponse().getStatus(),
                caseId + ": неожиданный статус для пароля длиной " + length
        );

        if (expectedStatus == 201) {
            assertSuccessfulRegister(result, dto);
        } else {
            assertRejected(result, expectedStatus);
        }
    }

    @DisplayName("Граничные значения длины displayName")
    @ParameterizedTest(name = "{0}: displayName длиной {1} -> HTTP {2}")
    @CsvSource({
            "R-15, 2, 400",
            "R-16, 3, 201",
            "R-17, 100, 201",
            "R-18, 101, 400"
    })
    void displayNameLengthBoundary(
            String caseId,
            int length,
            int expectedStatus
    ) throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto(
                uniqueEmail(),
                uniquePassword(),
                "a".repeat(length)
        );

        MvcResult result = register(dto);

        assertEquals(
                expectedStatus,
                result.getResponse().getStatus(),
                caseId + ": неожиданный статус для displayName длиной " + length
        );

        if (expectedStatus == 201) {
            assertSuccessfulRegister(result, dto);
        } else {
            assertRejected(result, expectedStatus);
        }
    }

    @Test
    @DisplayName("R-19: Запрос без Content-Type application/json отклоняется")
    void missingContentTypeIsRejected() throws Exception {
        RegisterRequestDto dto = uniqueRegisterRequestDto();
        String body = objectMapper.writeValueAsString(dto);

        MvcResult result = mockMvc.perform(
                post(REGISTER_URL).content(body)
        ).andReturn();

        assertRejected(result, 415);
    }

    @Test
    @DisplayName("R-20: Неизвестные поля JSON игнорируются, системный id не подменяется")
    void unknownJsonFieldsAreIgnored() throws Exception {
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
                """.formatted(
                injectedId,
                dto.email(),
                dto.password(),
                dto.displayName()
        );

        MvcResult result = registerRaw(body);

        assertSuccessfulRegister(result, dto);

        User saved = userRepository.findByEmailIgnoreCase(dto.email())
                .orElseThrow();

        assertNotEquals(
                injectedId,
                saved.getId(),
                "ID из запроса не должен попадать в сущность"
        );
    }
}