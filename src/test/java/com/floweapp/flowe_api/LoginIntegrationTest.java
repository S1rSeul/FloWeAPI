package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Login API")
class LoginIntegrationTest extends TestSupport {

    @Test
    @DisplayName("L-01: Валидные email и пароль возвращают токены")
    void validCredentialsReturnTokens() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email(),
                registrationDto.password()
        );

        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);
    }

    @Test
    @DisplayName("L-02: Неверный пароль возвращает HTTP 401")
    void wrongPasswordReturnsUnauthorized() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email(),
                "wrong-password"
        );

        MvcResult loginResult = login(loginDto);

        assertRejected(loginResult, 401);
    }

    @Test
    @DisplayName("L-03: Несуществующий email возвращает HTTP 401")
    void unknownEmailReturnsUnauthorized() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(
                uniqueEmail(),
                uniquePassword()
        );

        MvcResult loginResult = login(loginDto);

        assertRejected(loginResult, 401);
    }

    @Test
    @DisplayName("L-04: Email в другом регистре позволяет войти")
    void emailInDifferentCaseCanLogIn() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email().toUpperCase(),
                registrationDto.password()
        );

        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);
    }

    @Test
    @DisplayName("L-05: Email с пробелами по краям позволяет войти")
    void emailWithSurroundingSpacesCanLogIn() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        LoginRequestDto loginDto = new LoginRequestDto(
                " " + registrationDto.email() + " ",
                registrationDto.password()
        );

        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);
    }

    @Test
    @DisplayName("L-06: Пустой email возвращает HTTP 400")
    void emptyEmailReturnsBadRequest() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(
                "",
                uniquePassword()
        );

        assertRejected(login(loginDto), 400);
    }

    @Test
    @DisplayName("L-07: Пустой пароль возвращает HTTP 400")
    void emptyPasswordReturnsBadRequest() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(
                uniqueEmail(),
                ""
        );

        assertRejected(login(loginDto), 400);
    }

    @DisplayName("Некорректный формат email возвращает HTTP 400")
    @ParameterizedTest(name = "L-08: email \"{0}\" -> HTTP 400")
    @ValueSource(strings = {
            "not-an-email",
            "user",
            "user@",
            "@mail.com",
            "user@.com"
    })
    void malformedEmailReturnsBadRequest(String email) throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(
                email,
                uniquePassword()
        );

        assertRejected(login(loginDto), 400);
    }

    @Test
    @DisplayName("L-09: Два входа создают независимые refresh-токены")
    void twoLoginsHaveIndependentRefreshTokens() throws Exception {
        RegisterRequestDto registrationDto = uniqueRegisterRequestDto();

        MvcResult registration = registerSuccessfully(registrationDto);
        assertSuccessfulRegister(registration, registrationDto);

        LoginRequestDto loginDto = new LoginRequestDto(
                registrationDto.email(),
                registrationDto.password()
        );

        MvcResult deviceA = login(loginDto);
        MvcResult deviceB = login(loginDto);

        assertSuccessfulLogin(deviceA, loginDto);
        assertSuccessfulLogin(deviceB, loginDto);

        String refreshTokenA = token(deviceA, "refreshToken");
        String refreshTokenB = token(deviceB, "refreshToken");

        assertNotEquals(
                refreshTokenA,
                refreshTokenB,
                "У разных входов должны быть разные refresh-токены"
        );

        assertRefreshSucceeded(refresh(refreshTokenA));
        assertRefreshSucceeded(refresh(refreshTokenB));
    }
}