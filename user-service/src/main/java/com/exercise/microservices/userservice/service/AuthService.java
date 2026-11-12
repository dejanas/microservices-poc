package com.exercise.microservices.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.common.error.ConflictException;
import com.exercise.microservices.common.error.NotFoundException;
import com.exercise.microservices.userservice.dto.AuthResponse;
import com.exercise.microservices.userservice.dto.LoginRequest;
import com.exercise.microservices.userservice.dto.RegisterRequest;
import com.exercise.microservices.userservice.dto.UserResponse;
import com.exercise.microservices.userservice.model.Role;
import com.exercise.microservices.userservice.model.UserData;
import com.exercise.microservices.userservice.repository.UserRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static com.exercise.microservices.common.security.JwtSupport.ROLES;
import static com.exercise.microservices.common.security.JwtSupport.USER_ID;
import static com.exercise.microservices.common.security.JwtSupport.encode;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-seconds}")
    private long expirationSeconds;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken");
        }
        UserData user = UserData.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(Role.USER)))
                .build();
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username is already taken");
        }
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserData user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User was not found"));
        return AuthResponse.builder()
                .tokenType("Bearer")
                .token(generateToken(user))
                .userId(user.getId())
                .username(user.getUsername())
                .roles(Set.copyOf(user.getRoles()))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User was not found"));
    }

    private String generateToken(UserData user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(user.getUsername())
                .claim(USER_ID, user.getId())
                .claim(ROLES, user.getRoles().stream().map(Role::name).toList())
                .build();
        return encode(jwtEncoder, claims);
    }
}
