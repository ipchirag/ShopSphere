package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.dto.UserCreateRequest;
import com.chirag.shopsphere.dto.UserResponse;
import com.chirag.shopsphere.dto.UserUpdateRequest;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.exception.DuplicateResourceException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Plaintext passwords must not be stored. Because Spring Security / PasswordEncoder
        // is scheduled for the upcoming security phase, a locked/non-usable hash placeholder is assigned.
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password("{locked}account-awaiting-security-module")
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setFullName(request.getFullName());
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(false);
        userRepository.save(user);
    }
}
