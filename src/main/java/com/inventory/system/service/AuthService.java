package com.inventory.system.service;

import com.inventory.system.dto.request.LoginRequest;
import com.inventory.system.dto.response.AuthResponse;
import com.inventory.system.entity.User;
import com.inventory.system.exception.UnauthorizedException;
import com.inventory.system.repository.UserRepository;
import com.inventory.system.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = jwtTokenProvider.generateToken(authentication);

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .map(r -> r.replace("ROLE_", ""))
                    .orElse("UNKNOWN");

            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationMs())
                    .username(request.getUsername())
                    .role(role)
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
    }
}
