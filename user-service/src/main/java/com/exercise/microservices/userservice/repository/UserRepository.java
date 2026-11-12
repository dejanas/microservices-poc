package com.exercise.microservices.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.exercise.microservices.userservice.model.UserData;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserData, Long> {

    Optional<UserData> findByUsername(String username);

    boolean existsByUsername(String username);
}
