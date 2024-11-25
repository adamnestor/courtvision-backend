package com.adamnestor.courtvision.test;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import com.adamnestor.courtvision.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                usersRepository,
                passwordEncoder,
                jwtTokenUtil,
                authenticationManager
        );
    }
}