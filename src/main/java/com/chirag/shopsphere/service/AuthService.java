package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.AuthResponse;
import com.chirag.shopsphere.dto.LoginRequest;
import com.chirag.shopsphere.dto.RegisterRequest;
import com.chirag.shopsphere.dto.UserResponse;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.entity.UserRole;
import com.chirag.shopsphere.exception.BadRequestException;
import com.chirag.shopsphere.exception.DuplicateResourceException;
import com.chirag.shopsphere.repository.UserRepository;
import com.chirag.shopsphere.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new customer account.
     * - Rejects duplicate emails.
     * - Hashes password with BCrypt.
     * - Assigns CUSTOMER role (no self-registration as ADMIN).
     * - Returns JWT token immediately upon successful registration.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Build new user with hashed password and CUSTOMER role
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generate JWT for the newly registered user
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .accessToken(token)
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    /**
     * Authenticate an existing user with email and password.
     * - Uses Spring Security's AuthenticationManager for credential validation.
     * - Returns a generic error message on failure to prevent user enumeration.
     * - Returns JWT token on success.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            // Delegate authentication to Spring Security's AuthenticationManager
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // Generic message — do not reveal whether email exists or password is wrong
            throw new BadRequestException("Invalid email or password");
        }

        // If authentication passed, the user exists and credentials are valid
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated. Please contact support.");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
