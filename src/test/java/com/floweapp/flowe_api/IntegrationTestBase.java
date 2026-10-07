package com.floweapp.flowe_api;

import com.floweapp.flowe_api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
public abstract class IntegrationTestBase {

    protected static final String REGISTER_URL = "/api/v1/auth/register";
    protected static final String LOGIN_URL = "/api/v1/auth/login";
    protected static final String REFRESH_URL = "/api/v1/auth/refresh";
    protected static final String LOGOUT_URL = "/api/v1/auth/logout";
    protected static final String CREATE_COUPLE_URL = "/api/v1/couples";
    protected static final String GET_COUPLE_URL = "/api/v1/couples/me";
    protected static final String JOIN_COUPLE_URL = "/api/v1/couples/join";
    protected static final String UNKNOWN_INVITE_CODE = "0000000000";

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;
}