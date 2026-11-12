package com.exercise.microservices.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import com.exercise.microservices.userservice.model.Role;

import java.util.Set;

@Getter
@Builder
public class AuthResponse {

    private final String tokenType;
    private final String token;
    private final Long userId;
    private final String username;
    private final Set<Role> roles;
}
