package com.inventory.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles unauthenticated 401 requests with an RFC 7807 JSON problem detail.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.builder()
                .type("https://api.inventory-system.com/errors/unauthorized")
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Full authentication is required to access this resource.")
                .instance(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
