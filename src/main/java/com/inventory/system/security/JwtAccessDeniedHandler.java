package com.inventory.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles unauthorized 403 requests with an RFC 7807 JSON problem detail.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.builder()
                .type("https://api.inventory-system.com/errors/forbidden")
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("You do not have permission to access this resource.")
                .instance(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
