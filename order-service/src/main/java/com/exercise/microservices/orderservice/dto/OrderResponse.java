package com.exercise.microservices.orderservice.dto;

import lombok.Builder;
import lombok.Getter;
import com.exercise.microservices.orderservice.model.Order;

import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private final Long id;
    private final Long userId;
    private final List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(item -> new OrderItemResponse(item.getProductId(), item.getQuantity()))
                        .toList())
                .build();
    }
}
