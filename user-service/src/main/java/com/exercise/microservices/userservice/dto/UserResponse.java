package com.exercise.microservices.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import com.exercise.microservices.userservice.model.Role;
import com.exercise.microservices.userservice.model.UserData;

import java.util.Set;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String username;
    private final Set<Role> roles;

    public static UserResponse from(UserData user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .roles(Set.copyOf(user.getRoles()))
                .build();
    }
}
