package com.exercise.microservices.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.exercise.microservices.orderservice.model.Order;

import java.util.List;

public interface OrderRepository  extends JpaRepository<Order, Long> {

    List<Order> findAllByUserId(Long userId);
}
