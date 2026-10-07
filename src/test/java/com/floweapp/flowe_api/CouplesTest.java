package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.dto.JoinCoupleRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

public class CouplesTest extends TestSupport {
    // ---------------------------------------------------------------
    // CP-01: Успешное создание пространства
    // ---------------------------------------------------------------
    @Test
    void cp01_authenticatedUserCanCreateCouple() throws Exception {
        RegisterRequestDto registerDto = uniqueRegisterRequestDto();

        MvcResult registerResult = registerSuccessfully(registerDto);

        assertSuccessfulRegister(registerResult, registerDto);

        LoginRequestDto loginDto = new LoginRequestDto(registerDto.email(), registerDto.password());
        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);

        JsonNode tokens = responseJson(loginResult);

        String accessToken = tokens.path("accessToken").asString();

        CoupleNameRequestDto coupleDto = uniqueCoupleNameRequestDto();

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.partnerName").isEmpty())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        String coupleId = response.path("id").asString();

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.partnerName").isEmpty())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andReturn();
    }

    // ---------------------------------------------------------------
    // CP-02: Создание пространства без access-токена
    // ---------------------------------------------------------------
    @Test
    void cp02_createCoupleWithoutAccessTokenIsRejected() throws Exception {
        CoupleNameRequestDto dto = uniqueCoupleNameRequestDto();

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        assertRejected(result, 401);
    }

    // ---------------------------------------------------------------
    // CP-03: Создание пространства с невалидным access-токеном
    // ---------------------------------------------------------------
    @Test
    void cp03_createCoupleWithInvalidTokenIsRejected() throws Exception {
        CoupleNameRequestDto dto = uniqueCoupleNameRequestDto();

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        assertRejected(result, 401);
    }

    // ---------------------------------------------------------------
    // CP-04: Создание пространства с пустым именем
    // ---------------------------------------------------------------
    @Test
    void cp04_emptyCoupleNameIsRejected() throws Exception {
        CoupleNameRequestDto coupleDto = new CoupleNameRequestDto("");

        RegisterRequestDto registerDto = uniqueRegisterRequestDto();

        MvcResult registerResult = registerSuccessfully(registerDto);

        assertSuccessfulRegister(registerResult, registerDto);

        LoginRequestDto loginDto = new LoginRequestDto(registerDto.email(), registerDto.password());
        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);

        JsonNode tokens = responseJson(loginResult);

        String accessToken = tokens.path("accessToken").asString();

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupleDto)))
                .andReturn();

        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // CP-05...CP-06: Невалидное имя пространства
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "{0}: coupleName длиной {1} -> HTTP {2}")
    @CsvSource({
            "CP-05, 100, 201",
            "CP-06, 101, 400"
    })
    void cp05ToCp06_coupleNameLengthBoundary(String caseId, int length, int expectedStatus) throws Exception {
        RegisterRequestDto registerDto = uniqueRegisterRequestDto();

        MvcResult registerResult = registerSuccessfully(registerDto);

        assertSuccessfulRegister(registerResult, registerDto);

        LoginRequestDto loginDto = new LoginRequestDto(registerDto.email(), registerDto.password());
        MvcResult loginResult = login(loginDto);

        assertSuccessfulLogin(loginResult, loginDto);

        JsonNode tokens = responseJson(loginResult);

        String accessToken = tokens.path("accessToken").asString();

        CoupleNameRequestDto coupleDto = new CoupleNameRequestDto("a".repeat(length));
        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupleDto)))
                .andReturn();

        assertEquals(expectedStatus, result.getResponse().getStatus(),
                caseId + ": неожиданный статус для названия длиной " + length);

        if (expectedStatus == 201) {
            assertEquals("a".repeat(length), responseJson(result).path("name").asString());
        } else {
            assertRejected(result, expectedStatus);
        }
    }

    // ---------------------------------------------------------------
    // CP-07: Получение пространства, когда его нет
    // ---------------------------------------------------------------
    @Test
    void cp07_getCoupleWithoutCoupleReturns404() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult result = mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertRejected(result, 404);
    }

    // ---------------------------------------------------------------
    // CP-08: Получение пространства без токена
    // ---------------------------------------------------------------
    @Test
    void cp08_getCoupleWithoutTokenIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(get(GET_COUPLE_URL)).andReturn();

        assertRejected(result, 401);
    }

    // ---------------------------------------------------------------
    // CP-09: Повторное создание пространства тем же пользователем
    // ---------------------------------------------------------------
    @Test
    void cp09_userCannotCreateSecondCouple() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));
        CoupleNameRequestDto first = uniqueCoupleNameRequestDto();

        mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andReturn();

        assertRejected(result, 409);

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(first.name()));
    }

    // ---------------------------------------------------------------
    // CP-10: Успешное переименование пространства
    // ---------------------------------------------------------------
    @Test
    void cp10_userCanRenameCouple() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult created = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andExpect(status().isCreated())
                .andReturn();

        String coupleId = responseJson(created).path("id").asString();
        String inviteCode = responseJson(created).path("inviteCode").asString();
        CoupleNameRequestDto newName = uniqueCoupleNameRequestDto();

        mockMvc.perform(patch(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.name").value(newName.name()))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.inviteCode").value(inviteCode));

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName.name()));
    }

    // ---------------------------------------------------------------
    // CP-11: Переименование в пустое имя
    // ---------------------------------------------------------------
    @Test
    void cp11_renameCoupleToEmptyNameIsRejected() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));
        CoupleNameRequestDto original = uniqueCoupleNameRequestDto();

        mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(original)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(patch(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoupleNameRequestDto(""))))
                .andReturn();

        assertRejected(result, 400);

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.name").value(original.name()));
    }

    // ---------------------------------------------------------------
    // CP-12: Переименование без пространства
    // ---------------------------------------------------------------
    @Test
    void cp12_renameCoupleWithoutCoupleReturns404() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult result = mockMvc.perform(patch(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andReturn();

        assertRejected(result, 404);
    }

    // ---------------------------------------------------------------
    // CP-13: Переименование без токена
    // ---------------------------------------------------------------
    @Test
    void cp13_renameCoupleWithoutTokenIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(patch(GET_COUPLE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andReturn();

        assertRejected(result, 401);
    }

    // ---------------------------------------------------------------
    // CP-14: Успешное присоединение по инвайт-коду
    // ---------------------------------------------------------------
    @Test
    void cp14_partnerCanJoinCoupleByInviteCode() throws Exception {
        RegisterRequestDto ownerDto = uniqueRegisterRequestDto();
        RegisterRequestDto partnerDto = uniqueRegisterRequestDto();
        String ownerToken = accessTokenFrom(registerSuccessfully(ownerDto));
        String partnerToken = accessTokenFrom(registerSuccessfully(partnerDto));
        CoupleNameRequestDto coupleDto = uniqueCoupleNameRequestDto();

        MvcResult created = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupleDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String coupleId = responseJson(created).path("id").asString();
        String inviteCode = responseJson(created).path("inviteCode").asString();

        mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCoupleRequestDto(inviteCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.name").value(coupleDto.name()))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.partnerName").value(ownerDto.displayName()))
                .andExpect(jsonPath("$.inviteCode").isEmpty());

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupleId))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.partnerName").value(partnerDto.displayName()))
                .andExpect(jsonPath("$.inviteCode").isEmpty());
    }

    // ---------------------------------------------------------------
    // CP-15: Присоединение к собственному пространству
    // ---------------------------------------------------------------
    @Test
    void cp15_ownerCannotJoinOwnCouple() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult created = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andExpect(status().isCreated())
                .andReturn();

        String inviteCode = responseJson(created).path("inviteCode").asString();

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCoupleRequestDto(inviteCode))))
                .andReturn();

        assertRejected(result, 409);

        mockMvc.perform(get(GET_COUPLE_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    // ---------------------------------------------------------------
    // CP-16: Присоединение по несуществующему коду
    // ---------------------------------------------------------------
    @Test
    void cp16_joinWithUnknownInviteCodeReturns404() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCoupleRequestDto("0".repeat(10)))))
                .andReturn();

        assertRejected(result, 404);
    }

    // ---------------------------------------------------------------
    // CP-17: Присоединение с пустым кодом
    // ---------------------------------------------------------------
    @Test
    void cp17_joinWithEmptyInviteCodeIsRejected() throws Exception {
        String token = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCoupleRequestDto(""))))
                .andReturn();

        assertRejected(result, 400);
    }

    // ---------------------------------------------------------------
    // CP-18: Присоединение без токена
    // ---------------------------------------------------------------
    @Test
    void cp18_joinWithoutTokenIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinCoupleRequestDto("ZZZZZZZZ"))))
                .andReturn();

        assertRejected(result, 401);
    }

    // ---------------------------------------------------------------
    // CP-19: Код нельзя использовать повторно (после join он удаляется)
    // ---------------------------------------------------------------
    @Test
    void cp19_usedInviteCodeCannotBeReused() throws Exception {
        String ownerToken = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));
        String partnerToken = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));
        String thirdToken = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult created = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andExpect(status().isCreated())
                .andReturn();

        String joinBody = objectMapper.writeValueAsString(
                new JoinCoupleRequestDto(responseJson(created).path("inviteCode").asString()));

        mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + partnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + thirdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody))
                .andReturn();

        assertRejected(result, 404);
    }

    // ---------------------------------------------------------------
    // CP-20: Пользователь с пространством не может присоединиться к другому
    // ---------------------------------------------------------------
    @Test
    void cp20_userWithCoupleCannotJoinAnotherCouple() throws Exception {
        String ownerToken = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));
        String otherToken = accessTokenFrom(registerSuccessfully(uniqueRegisterRequestDto()));

        MvcResult created = mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post(CREATE_COUPLE_URL)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueCoupleNameRequestDto())))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post(CREATE_COUPLE_URL + "/join")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JoinCoupleRequestDto(responseJson(created).path("inviteCode").asString()))))
                .andReturn();

        assertRejected(result, 409);
    }
}
