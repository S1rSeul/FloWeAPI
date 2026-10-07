package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Couples API")
public class CouplesTest extends TestSupport {

    private static final String ALREADY_IN_COUPLE = "Пользователь уже находится в паре";
    private static final String CANNOT_JOIN_OWN = "Вы не можете присоединиться к своей же паре";
    private static final String COUPLE_NOT_FOUND = "Пользователь не состоит в паре";
    private static final String INVITE_NOT_FOUND = "Invite-код не найден";

    @Test
    @DisplayName("CP-01: Пользователь с access-токеном создаёт пространство и видит его в GET /me")
    void authenticatedUserCanCreateCouple() throws Exception {
        String token = registerAndGetAccessToken();
        CoupleNameRequestDto coupleDto = uniqueCoupleNameRequestDto();

        MvcResult created = createCouple(token, coupleDto)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.partnerName").isEmpty())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andReturn();

        getMyCouple(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(responseJson(created).path("id").asString()))
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.partnerName").isEmpty())
                .andExpect(jsonPath("$.inviteCode").exists());
    }

    @DisplayName("Запрос без access-токена отклоняется")
    @ParameterizedTest(name = "{0}: {1} {2} без токена -> 401")
    @CsvSource({
            "CP-02, POST,  /api/v1/couples",
            "CP-08, GET,   /api/v1/couples/me",
            "CP-13, PATCH, /api/v1/couples/me",
            "CP-18, POST,  /api/v1/couples/join"
    })
    void requestWithoutTokenIsRejected(String caseId, String method, String url) throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertRejected(result, 401);
    }

    @DisplayName("Запрос с невалидным access-токеном отклоняется")
    @ParameterizedTest(name = "{0}: {1} {2} с невалидным токеном -> 401")
    @CsvSource({
            "CP-03,  POST,  /api/v1/couples",
            "CP-03a, GET,   /api/v1/couples/me",
            "CP-03b, PATCH, /api/v1/couples/me",
            "CP-03c, POST,  /api/v1/couples/join"
    })
    void requestWithInvalidTokenIsRejected(String caseId, String method, String url) throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), url)
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertRejected(result, 401);
    }

    @Test
    @DisplayName("CP-04: Создание пространства с пустым именем даёт 400")
    void emptyCoupleNameIsRejected() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(createCouple(token, new CoupleNameRequestDto("")).andReturn(), 400);
    }

    @DisplayName("Граница длины имени пространства")
    @ParameterizedTest(name = "{0}: имя длиной {1} -> HTTP {2}")
    @CsvSource({
            "CP-05, 100, 201",
            "CP-06, 101, 400"
    })
    void coupleNameLengthBoundary(String caseId, int length, int expectedStatus) throws Exception {
        String token = registerAndGetAccessToken();
        String name = "a".repeat(length);

        MvcResult result = createCouple(token, new CoupleNameRequestDto(name)).andReturn();

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для названия длиной " + length);

        if (expectedStatus == 201) {
            assertEquals(name, responseJson(result).path("name").asString());
        } else {
            assertRejected(result, expectedStatus);
        }
    }

    @Test
    @DisplayName("CP-07: GET /me без пространства даёт 404")
    void getCoupleWithoutCoupleReturns404() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(getMyCouple(token)
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("CP-09: Повторное создание пространства даёт 409 и не меняет имя")
    void userCannotCreateSecondCouple() throws Exception {
        String token = registerAndGetAccessToken();
        CoupleNameRequestDto first = uniqueCoupleNameRequestDto();
        createCoupleSuccessfully(token, first);

        assertRejected(createCouple(token, uniqueCoupleNameRequestDto())
                .andExpect(jsonPath("$.message").value(ALREADY_IN_COUPLE))
                .andReturn(), 409);

        getMyCouple(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(first.name()));
    }

    @Test
    @DisplayName("CP-10: Пользователь переименовывает пространство, id и инвайт-код сохраняются")
    void userCanRenameCouple() throws Exception {
        String token = registerAndGetAccessToken();
        JsonNode created = createCoupleSuccessfully(token, uniqueCoupleNameRequestDto());
        CoupleNameRequestDto newName = uniqueCoupleNameRequestDto();

        updateMyCouple(token, newName)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.path("id").asString()))
                .andExpect(jsonPath("$.name").value(newName.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.inviteCode").value(created.path("inviteCode").asString()));

        getMyCouple(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName.name()));
    }

    @Test
    @DisplayName("CP-11: Переименование в пустое имя даёт 400 и не меняет имя")
    void renameCoupleToEmptyNameIsRejected() throws Exception {
        String token = registerAndGetAccessToken();
        CoupleNameRequestDto original = uniqueCoupleNameRequestDto();
        createCoupleSuccessfully(token, original);

        assertRejected(updateMyCouple(token, new CoupleNameRequestDto("")).andReturn(), 400);

        getMyCouple(token).andExpect(jsonPath("$.name").value(original.name()));
    }

    @Test
    @DisplayName("CP-12: Переименование без пространства даёт 404")
    void renameCoupleWithoutCoupleReturns404() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(updateMyCouple(token, uniqueCoupleNameRequestDto())
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("CP-14: Партнёр присоединяется по инвайт-коду, пространство становится active")
    void partnerCanJoinCoupleByInviteCode() throws Exception {
        RegisterRequestDto ownerDto = uniqueRegisterRequestDto();
        RegisterRequestDto partnerDto = uniqueRegisterRequestDto();
        String ownerToken = accessTokenFrom(registerSuccessfully(ownerDto));
        String partnerToken = accessTokenFrom(registerSuccessfully(partnerDto));
        CoupleNameRequestDto coupleDto = uniqueCoupleNameRequestDto();

        JsonNode created = createCoupleSuccessfully(ownerToken, coupleDto);
        String coupleId = created.path("id").asString();

        joinCouple(partnerToken, created.path("inviteCode").asString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.partnerName").value(ownerDto.displayName()))
                .andExpect(jsonPath("$.inviteCode").isEmpty());

        getMyCouple(ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.partnerName").value(partnerDto.displayName()))
                .andExpect(jsonPath("$.inviteCode").isEmpty());
    }

    @Test
    @DisplayName("CP-15: Владелец не может присоединиться к собственному пространству")
    void ownerCannotJoinOwnCouple() throws Exception {
        String token = registerAndGetAccessToken();
        JsonNode created = createCoupleSuccessfully(token, uniqueCoupleNameRequestDto());

        assertRejected(joinCouple(token, created.path("inviteCode").asString())
                .andExpect(jsonPath("$.message").value(CANNOT_JOIN_OWN))
                .andReturn(), 409);

        getMyCouple(token).andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    @DisplayName("CP-16: Присоединение по несуществующему коду даёт 404")
    void joinWithUnknownInviteCodeReturns404() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(joinCouple(token, UNKNOWN_INVITE_CODE)
                .andExpect(jsonPath("$.message").value(INVITE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("CP-17: Присоединение с пустым кодом даёт 400")
    void joinWithEmptyInviteCodeIsRejected() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(joinCouple(token, "").andReturn(), 400);
    }

    @Test
    @DisplayName("CP-19: Использованный инвайт-код нельзя применить повторно")
    void usedInviteCodeCannotBeReused() throws Exception {
        String ownerToken = registerAndGetAccessToken();
        String partnerToken = registerAndGetAccessToken();
        String thirdToken = registerAndGetAccessToken();
        String inviteCode = createCoupleSuccessfully(ownerToken, uniqueCoupleNameRequestDto())
                .path("inviteCode").asString();

        joinCouple(partnerToken, inviteCode).andExpect(status().isOk());

        assertRejected(joinCouple(thirdToken, inviteCode)
                .andExpect(jsonPath("$.message").value(INVITE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("CP-20: Пользователь с пространством не может присоединиться к другому")
    void userWithCoupleCannotJoinAnotherCouple() throws Exception {
        String ownerToken = registerAndGetAccessToken();
        String otherToken = registerAndGetAccessToken();
        String inviteCode = createCoupleSuccessfully(ownerToken, uniqueCoupleNameRequestDto())
                .path("inviteCode").asString();
        createCoupleSuccessfully(otherToken, uniqueCoupleNameRequestDto());

        assertRejected(joinCouple(otherToken, inviteCode)
                .andExpect(jsonPath("$.message").value(ALREADY_IN_COUPLE))
                .andReturn(), 409);
    }
}