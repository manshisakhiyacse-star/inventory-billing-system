package com.inventory.system.service;

import com.inventory.system.dto.request.CreateUserRequest;
import com.inventory.system.dto.response.UserResponse;
import com.inventory.system.entity.User;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.UserMapper;
import com.inventory.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);

        User savedUser = userRepository.save(user);

        auditService.logAction("CREATE_USER", "User", savedUser.getId(),
                "Created user with username: " + savedUser.getUsername() + ", role: " + savedUser.getRole());

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setUserActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setActive(active);
        User updated = userRepository.save(user);

        auditService.logAction(active ? "ACTIVATE_USER" : "DEACTIVATE_USER", "User", updated.getId(),
                "Set active status to: " + active);

        return userMapper.toResponse(updated);
    }
}
