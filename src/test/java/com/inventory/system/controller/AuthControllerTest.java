package com.inventory.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.request.LoginRequest;
import com.inventory.system.dto.response.AuthResponse;
import com.inventory.system.exception.GlobalExceptionHandler;
import com.inventory.system.exception.UnauthorizedException;
import com.inventory.system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with JWT on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");
        AuthResponse response = AuthResponse.builder()
                .token("jwt.token.here")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .username("admin")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 400 when body fails validation (blank username)")
    void testLoginValidationFailure() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 when credentials are invalid")
    void testLoginUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(authService.login(any(LoginRequest.class))).thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
