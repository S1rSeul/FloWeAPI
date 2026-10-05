package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
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
    void cp03_authenticatedUserCanCreateCouple() throws Exception {
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
    void invalidCoupleNameIsRejected(String caseId, int length, int expectedStatus) throws Exception {
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

        if (expectedStatus != 201) {
            assertRejected(result, expectedStatus);
        }
    }
}
