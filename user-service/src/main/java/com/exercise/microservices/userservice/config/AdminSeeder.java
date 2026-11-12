package com.exercise.microservices.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.userservice.model.Role;
import com.exercise.microservices.userservice.model.UserData;
import com.exercise.microservices.userservice.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        userRepository.save(UserData.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles(new HashSet<>(Set.of(Role.USER, Role.ADMIN)))
                .build());
        log.info("Created demo admin user '{}'", adminUsername);
    }
}
