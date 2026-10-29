package com.inventory.system.service;

import com.inventory.system.dto.request.LoginRequest;
import com.inventory.system.dto.response.AuthResponse;
import com.inventory.system.exception.UnauthorizedException;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should return AuthResponse with JWT token on successful login")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        Authentication auth = mock(Authentication.class);

        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(auth).getAuthorities();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when credentials are bad")
    void testLoginBadCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
