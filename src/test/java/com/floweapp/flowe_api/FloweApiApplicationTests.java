package com.floweapp.flowe_api;

import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.couple.dto.CreateCoupleRequestDto;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FloweApiApplicationTests extends IntegrationTestBase {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void healthShouldReturnUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	private String uniqueEmail() {
		return "test-" + UUID.randomUUID() + "@example.com";
	}

	private MvcResult register(String email, String password, String displayName) throws Exception {
		RegisterRequestDto request = new RegisterRequestDto(email, password, displayName);

		String body = objectMapper.writeValueAsString(request);

		return mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
						.andReturn();
	}

	private MvcResult login(String email, String password) throws Exception {
		LoginRequestDto request = new LoginRequestDto(email, password);

		String body = objectMapper.writeValueAsString(request);

		return mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
						.andReturn();
	}

	@Test
	void registerCreatesUserAndReturnsTokens() throws Exception {
		String email = uniqueEmail();
		String password = "password123";

		MvcResult result = register(email, password, "Roma Artavodov");

		assertEquals(201, result.getResponse().getStatus());

		JsonNode response = objectMapper.readTree(
				result.getResponse().getContentAsString()
		);
		assertFalse(response.path("accessToken").asString().isBlank());
		assertFalse(response.path("refreshToken").asString().isBlank());

		User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
		assertEquals("Roma Artavodov", saved.getDisplayName());
		assertTrue(passwordEncoder.matches(password, saved.getPasswordHash()));
	}

	@Test
	void loginWithCorrectPasswordReturnsTokens() throws Exception {
		String email = uniqueEmail();
		String password = "password123";

		register(email, password, "Danissimo");
		Thread.sleep(1000);
		MvcResult result = login(email, password);

		assertEquals(200, result.getResponse().getStatus());

		JsonNode response = objectMapper.readTree(
				result.getResponse().getContentAsString()
		);
		assertFalse(response.path("accessToken").asString().isBlank());
		assertFalse(response.path("refreshToken").asString().isBlank());
	}

	@Test
	void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
		String email = uniqueEmail();

		register(email, "password123", "Micael Zimin");
		MvcResult result = login(email, "wrong-password");

		assertEquals(401, result.getResponse().getStatus());
	}

}
