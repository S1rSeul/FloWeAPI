package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LoginIntegrationTest extends TestSupport {

    // ---------------------------------------------------------------
    // L-01: Валидные email + пароль
    // ---------------------------------------------------------------
    @Test
    void l01_validCredentialsReturnTokens() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        LoginRequestDto loginRequestDto = new LoginRequestDto(registerRequestDtoDto.email(), registerRequestDtoDto.password());
        MvcResult resultLogin = login(loginRequestDto);

        assertSuccessfulLogin(resultLogin, loginRequestDto);
    }

    // ---------------------------------------------------------------
    // L-02: Неверный пароль
    // ---------------------------------------------------------------
    @Test
    void l02_wrongPasswordReturnsUnauthorized() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        LoginRequestDto loginRequestDto = new LoginRequestDto(registerRequestDtoDto.email(), "password");
        MvcResult resultLogin = login(loginRequestDto);

        assertRejected(resultLogin, 401);
    }

    // ---------------------------------------------------------------
    // L-03: Несуществующий email
    // ---------------------------------------------------------------
    @Test
    void l03_unknownEmailReturnsUnauthorized() throws Exception {
        LoginRequestDto loginRequestDto = new LoginRequestDto(uniqueEmail(), uniquePassword());
        MvcResult resultLogin = login(loginRequestDto);

        assertRejected(resultLogin, 401);
    }

    // ---------------------------------------------------------------
    // L-04: Email в другом регистре
    // ---------------------------------------------------------------
    @Test
    void l04_emailInDifferentCaseCanLogIn() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        LoginRequestDto loginRequestDto = new LoginRequestDto(registerRequestDtoDto.email().toUpperCase(), registerRequestDtoDto.password());
        MvcResult resultLogin = login(loginRequestDto);

        assertSuccessfulLogin(resultLogin, loginRequestDto);
    }

    // ---------------------------------------------------------------
    // L-05: Email с пробелами по краям
    // ---------------------------------------------------------------
    @Test
    void l05_emailWithSurroundingSpacesCanLogIn() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        LoginRequestDto loginRequestDto = new LoginRequestDto(" " + registerRequestDtoDto.email() + " ", registerRequestDtoDto.password());
        MvcResult resultLogin = login(loginRequestDto);

        assertSuccessfulLogin(resultLogin, loginRequestDto);
    }

    // ---------------------------------------------------------------
    // L-06: Пустой email
    // ---------------------------------------------------------------
    @Test
    void l06_emptyEmailReturnsBadRequest() throws Exception {
        LoginRequestDto loginRequestDto = new LoginRequestDto("", uniquePassword());
        MvcResult resultLogin = login(loginRequestDto);

        assertRejected(resultLogin, 400);
    }

    // ---------------------------------------------------------------
    // L-07: Пустой пароль
    // ---------------------------------------------------------------
    @Test
    void l07_emptyPasswordReturnsBadRequest() throws Exception {
        LoginRequestDto loginRequestDto = new LoginRequestDto(uniqueEmail(), "");
        MvcResult resultLogin = login(loginRequestDto);

        assertRejected(resultLogin, 400);
    }

    // ---------------------------------------------------------------
    // L-08: Некорректный формат email
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "L-08: Некорректный email ''{0}''")
    @ValueSource(strings = {"not-an-email", "user", "user@", "@mail.com", "user@.com"})
    void l08_malformedEmailReturnsBadRequest(String email) throws Exception {
        LoginRequestDto loginRequestDto = new LoginRequestDto(email, uniquePassword());
        MvcResult resultLogin = login(loginRequestDto);

        assertRejected(resultLogin, 400);
    }

    // ---------------------------------------------------------------
    // L-09: Вход с устройства A и устройства B
    // ---------------------------------------------------------------
    @Test
    void l09_twoLoginsHaveIndependentRefreshTokens() throws Exception {
        RegisterRequestDto registerRequestDtoDto = uniqueRegisterRequestDto();

        MvcResult resultRegister = registerSuccessfully(registerRequestDtoDto);
        assertSuccessfulRegister(resultRegister, registerRequestDtoDto);

        LoginRequestDto loginRequestDto = new LoginRequestDto(registerRequestDtoDto.email(), registerRequestDtoDto.password());

        MvcResult deviceA = login(loginRequestDto);
        MvcResult deviceB = login(loginRequestDto);

        assertSuccessfulLogin(deviceA,  loginRequestDto);
        assertSuccessfulLogin(deviceB, loginRequestDto);

        String refreshTokenA = token(deviceA, "refreshToken");
        String refreshTokenB = token(deviceB, "refreshToken");

        assertNotEquals(refreshTokenA, refreshTokenB, "У устройств должны быть разные refresh-токены");

        assertRefreshSucceeded(refresh(refreshTokenA));
        assertRefreshSucceeded(refresh(refreshTokenB));
    }
}
