package com.exercise.microservices.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.common.error.NotFoundException;
import com.exercise.microservices.orderservice.dto.OrderResponse;
import com.exercise.microservices.orderservice.model.Order;
import com.exercise.microservices.orderservice.model.OrderItem;
import com.exercise.microservices.orderservice.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderPersistence {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse insert(Long userId, List<OrderItem> items) {
        Order order = Order.builder()
                .userId(userId)
                .items(new ArrayList<>(items))
                .build();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse require(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new NotFoundException("Order " + id + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse replaceItems(Long id, List<OrderItem> items) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " was not found"));
        order.getItems().clear();
        order.getItems().addAll(items);
        return OrderResponse.from(order);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new NotFoundException("Order " + id + " was not found");
        }
        orderRepository.deleteById(id);
    }
}
