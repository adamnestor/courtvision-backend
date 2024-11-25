package com.adamnestor.courtvision.security.service;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserRole;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.security.dto.AuthResponse;
import com.adamnestor.courtvision.security.dto.LoginRequest;
import com.adamnestor.courtvision.security.dto.RegisterRequest;
import com.adamnestor.courtvision.security.exception.EmailAlreadyExistsException;
import com.adamnestor.courtvision.security.exception.InvalidCredentialsException;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}