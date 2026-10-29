package com.inventory.system.controller;

import com.inventory.system.dto.request.CreateUserRequest;
import com.inventory.system.dto.response.UserResponse;
import com.inventory.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin-only endpoints for managing application users and credentials")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Admin only. Registers a new user with an assigned system role.")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Admin only. Returns all registered users.")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Admin only. Returns user details by ID.")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Admin only. Disables user account so they cannot log in.")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setUserActiveStatus(id, false));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Admin only. Enables a previously deactivated user account.")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setUserActiveStatus(id, true));
    }
}
