package com.exercise.microservices.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.exercise.microservices.common.error.BadRequestException;
import com.exercise.microservices.common.security.CurrentUser;
import com.exercise.microservices.orderservice.dto.OrderItemRequest;
import com.exercise.microservices.orderservice.dto.OrderItemResponse;
import com.exercise.microservices.orderservice.dto.OrderRequest;
import com.exercise.microservices.orderservice.dto.OrderResponse;
import com.exercise.microservices.orderservice.model.OrderItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderPersistence persistence;
    private final ProductClient productClient;

    public OrderResponse create(OrderRequest request) {
        Long userId = CurrentUser.id();
        List<OrderItem> items = normalize(request.getItems());
        List<OrderItem> reserved = reserve(items);
        try {
            OrderResponse created = persistence.insert(userId, items);
            log.info("Created order {} for user {}", created.getId(), created.getUserId());
            return created;
        } catch (RuntimeException exception) {
            release(reserved);
            throw exception;
        }
    }

    public OrderResponse getById(Long id) {
        OrderResponse order = persistence.require(id);
        CurrentUser.assertOwnerOrAdmin(order.getUserId());
        return order;
    }

    public List<OrderResponse> getForCurrentUser() {
        return persistence.findByUserId(CurrentUser.id());
    }

    public List<OrderResponse> getForUser(Long userId) {
        CurrentUser.assertOwnerOrAdmin(userId);
        return persistence.findByUserId(userId);
    }

    public OrderResponse update(Long id, OrderRequest request) {
        OrderResponse existing = persistence.require(id);
        CurrentUser.assertOwnerOrAdmin(existing.getUserId());
        List<OrderItem> items = normalize(request.getItems());
        List<OrderItem> applied = applyStockChange(existing.getItems(), items);
        try {
            OrderResponse updated = persistence.replaceItems(id, items);
            log.info("Order {} is updated", updated.getId());
            return updated;
        } catch (RuntimeException exception) {
            undo(applied);
            throw exception;
        }
    }

    public void delete(Long id) {
        OrderResponse existing = persistence.require(id);
        CurrentUser.assertOwnerOrAdmin(existing.getUserId());
        List<OrderItem> released = existing.getItems().stream()
                .map(item -> OrderItem.builder().productId(item.getProductId()).quantity(item.getQuantity()).build())
                .toList();
        release(released);
        try {
            persistence.delete(id);
            log.info("Order {} is deleted", id);
        } catch (RuntimeException exception) {
            reserve(released);
            throw exception;
        }
    }

    private List<OrderItem> reserve(List<OrderItem> items) {
        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OrderItem item : items) {
                productClient.reserve(item.getProductId(), item.getQuantity());
                reserved.add(item);
            }
            return reserved;
        } catch (RuntimeException exception) {
            release(reserved);
            throw exception;
        }
    }

    private void release(List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                productClient.release(item.getProductId(), item.getQuantity());
            } catch (RuntimeException exception) {
                log.warn("Could not release {} units of product {}", item.getQuantity(), item.getProductId(), exception);
            }
        }
    }

    /**
     * Positive quantities were reserved and negative ones were released.
     * The returned list is what {@link #undo} reverses if saving the order fails.
     */
    private List<OrderItem> applyStockChange(List<OrderItemResponse> previous, List<OrderItem> next) {
        Map<Long, Integer> delta = new LinkedHashMap<>();
        for (OrderItemResponse item : previous) {
            delta.merge(item.getProductId(), -item.getQuantity(), OrderService::addQuantity);
        }
        for (OrderItem item : next) {
            delta.merge(item.getProductId(), item.getQuantity(), OrderService::addQuantity);
        }
        List<Map.Entry<Long, Integer>> changes = delta.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Comparator.comparingInt(entry -> entry.getValue() > 0 ? 0 : 1))
                .toList();
        List<OrderItem> applied = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> change : changes) {
                if (change.getValue() > 0) {
                    productClient.reserve(change.getKey(), change.getValue());
                } else {
                    productClient.release(change.getKey(), -change.getValue());
                }
                applied.add(OrderItem.builder().productId(change.getKey()).quantity(change.getValue()).build());
            }
            return applied;
        } catch (RuntimeException exception) {
            undo(applied);
            throw exception;
        }
    }

    private void undo(List<OrderItem> applied) {
        for (int index = applied.size() - 1; index >= 0; index--) {
            OrderItem change = applied.get(index);
            try {
                if (change.getQuantity() > 0) {
                    productClient.release(change.getProductId(), change.getQuantity());
                } else if (change.getQuantity() < 0) {
                    productClient.reserve(change.getProductId(), -change.getQuantity());
                }
            } catch (RuntimeException exception) {
                log.warn("Could not undo a stock change for product {}", change.getProductId(), exception);
            }
        }
    }

    private static List<OrderItem> normalize(List<OrderItemRequest> requests) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest request : requests) {
            quantities.merge(request.getProductId(), request.getQuantity(), OrderService::addQuantity);
        }
        List<OrderItem> items = new ArrayList<>();
        quantities.forEach((productId, quantity) ->
                items.add(OrderItem.builder().productId(productId).quantity(quantity).build()));
        return items;
    }

    private static int addQuantity(int left, int right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Requested quantity is too large");
        }
    }
}
