package com.floweapp.flowe_api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LoginIntegrationTest extends AuthTestSupport {

    // ---------------------------------------------------------------
    // L-01: Валидные email + пароль
    // ---------------------------------------------------------------
    @Test
    void l01_validCredentialsReturnTokens() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        registerSuccessfully(email, password);

        assertLoginSucceeded(login(email, password));
    }

    // ---------------------------------------------------------------
    // L-02: Неверный пароль
    // ---------------------------------------------------------------
    @Test
    void l02_wrongPasswordReturnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        registerSuccessfully(email, password);

        assertLoginRejected(login(email, "wrong-password"), 401);
    }

    // ---------------------------------------------------------------
    // L-03: Несуществующий email
    // ---------------------------------------------------------------
    @Test
    void l03_unknownEmailReturnsUnauthorized() throws Exception {
        assertLoginRejected(login(uniqueEmail(), uniquePassword()), 401);
    }

    // ---------------------------------------------------------------
    // L-04: Email в другом регистре
    // ---------------------------------------------------------------
    @Test
    void l04_emailInDifferentCaseCanLogIn() throws Exception {
        String email = uniqueEmail().toLowerCase();
        String password = uniquePassword();

        registerSuccessfully(email, password);

        assertLoginSucceeded(login(email.toUpperCase(), password));
    }

    // ---------------------------------------------------------------
    // L-05: Email с пробелами по краям
    // ---------------------------------------------------------------
    @Test
    void l05_emailWithSurroundingSpacesCanLogIn()
            throws Exception {
        String cleanEmail = uniqueEmail();
        String password = uniquePassword();

        registerSuccessfully(cleanEmail, password);

        MvcResult result = loginRawFields("  " + cleanEmail + "  ", password);

        assertLoginSucceeded(result);
    }

    // ---------------------------------------------------------------
    // L-06: Пустой email
    // ---------------------------------------------------------------
    @Test
    void l06_emptyEmailReturnsBadRequest() throws Exception {
        assertLoginRejected(login("", uniquePassword()), 400);
    }

    // ---------------------------------------------------------------
    // L-07: Пустой пароль
    // ---------------------------------------------------------------
    @Test
    void l07_emptyPasswordReturnsBadRequest() throws Exception {
        assertLoginRejected(login(uniqueEmail(), ""), 400);
    }

    // ---------------------------------------------------------------
    // L-08: Некорректный формат email
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "L-08: Некорректный email ''{0}''")
    @ValueSource(strings = {"not-an-email", "user", "user@", "@mail.com", "user@.com"})
    void l08_malformedEmailReturnsBadRequest(String email) throws Exception {
        assertLoginRejected(login(email, uniquePassword()), 400);
    }

    // ---------------------------------------------------------------
    // L-09: Вход с устройства A и устройства B
    // ---------------------------------------------------------------
    @Test
    void l09_twoLoginsHaveIndependentRefreshTokens() throws Exception {
        String email = uniqueEmail();
        String password = uniquePassword();

        registerSuccessfully(email, password);

        MvcResult deviceA = login(email, password);
        MvcResult deviceB = login(email, password);

        assertLoginSucceeded(deviceA);
        assertLoginSucceeded(deviceB);

        String refreshTokenA = token(deviceA, "refreshToken");
        String refreshTokenB = token(deviceB, "refreshToken");

        assertNotEquals(refreshTokenA, refreshTokenB, "У устройств должны быть разные refresh-токены");

        assertRefreshSucceeded(refresh(refreshTokenA));
        assertRefreshSucceeded(refresh(refreshTokenB));
    }
}
