package com.adamnestor.courtvision.security.service;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.domain.UserStatus;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.exception.PasswordMismatchException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthenticationService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenUtil jwtTokenUtil,
            AuthenticationManager authenticationManager) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        // Validate registration data
        validateRegistration(request);

        // Create new user
        Users newUser = createUser(request);

        // Save user
        Users savedUser = usersRepository.save(newUser);

        // Generate JWT token
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(savedUser.getEmail())
                .password(savedUser.getPasswordHash())
                .authorities("ROLE_" + savedUser.getRole().name())
                .build();

        String token = jwtTokenUtil.generateToken(userDetails);

        // Return authentication response
        return createAuthResponse(savedUser, token);
    }

    private void validateRegistration(RegisterRequest request) {
        // Check if email already exists
        if (usersRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        // Check if passwords match
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }
    }

    private Users createUser(RegisterRequest request) {
        Users user = new Users();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        return user;
    }

    private AuthResponse createAuthResponse(Users user, String token) {
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            // Get user from database
            Users user = usersRepository.findByEmail(request.email())
                    .orElseThrow(() -> new InvalidCredentialsException());

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            usersRepository.save(user);

            // Create UserDetails for token generation
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities("ROLE_" + user.getRole().name())
                    .build();

            // Generate JWT token
            String token = jwtTokenUtil.generateToken(userDetails);

            // Return authentication response
            return createAuthResponse(user, token);

        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
    }
}