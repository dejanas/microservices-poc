package com.exercise.microservices.orderservice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.exercise.microservices.common.error.BadRequestException;
import com.exercise.microservices.common.security.JwtSupport;
import com.exercise.microservices.orderservice.dto.OrderItemRequest;
import com.exercise.microservices.orderservice.dto.OrderRequest;
import com.exercise.microservices.orderservice.dto.OrderResponse;
import com.exercise.microservices.orderservice.model.OrderItem;
import com.exercise.microservices.orderservice.service.OrderPersistence;
import com.exercise.microservices.orderservice.service.OrderService;
import com.exercise.microservices.orderservice.service.ProductClient;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderPersistence persistence;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        authenticate(7L, "ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAnOrderForTheAuthenticatedUserWhenStockIsEnough() {
        when(persistence.insert(eq(7L), any())).thenReturn(OrderResponse.builder().id(99L).userId(7L).items(List.of()).build());

        OrderRequest request = OrderRequest.builder()
                .items(List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(1L, 2)))
                .build();

        OrderResponse created = orderService.create(request);

        assertEquals(99L, created.getId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> items = ArgumentCaptor.forClass(List.class);
        verify(persistence).insert(eq(7L), items.capture());
        assertEquals(1, items.getValue().size());
        assertEquals(4, items.getValue().get(0).getQuantity());
        verify(productClient).reserve(1L, 4);
    }

    @Test
    void rejectsAnOrderWhenStockIsTooLow() {
        doThrow(new BadRequestException("Product 1 does not have enough stock"))
                .when(productClient).reserve(1L, 2);
        OrderRequest request = OrderRequest.builder()
                .items(List.of(new OrderItemRequest(1L, 2)))
                .build();

        assertThrows(BadRequestException.class, () -> orderService.create(request));
        verify(persistence, never()).insert(any(), any());
    }

    @Test
    void releasesStockAlreadyReservedWhenALaterProductIsMissing() {
        doNothing().when(productClient).reserve(1L, 2);
        doThrow(new BadRequestException("Product 2 does not exist"))
                .when(productClient).reserve(2L, 1);
        OrderRequest request = OrderRequest.builder()
                .items(List.of(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1)))
                .build();

        assertThrows(BadRequestException.class, () -> orderService.create(request));
        verify(productClient).release(1L, 2);
        verify(persistence, never()).insert(any(), any());
    }

    @Test
    void hidesAnotherUsersOrder() {
        when(persistence.require(5L)).thenReturn(OrderResponse.builder().id(5L).userId(8L).items(List.of()).build());

        assertThrows(AccessDeniedException.class, () -> orderService.getById(5L));
    }

    @Test
    void letsAnAdminReadAnyOrder() {
        authenticate(1L, "ROLE_ADMIN");
        when(persistence.require(5L)).thenReturn(OrderResponse.builder().id(5L).userId(8L).items(List.of()).build());

        OrderResponse order = orderService.getById(5L);

        assertEquals(8L, order.getUserId());
    }

    private static void authenticate(Long userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-" + userId)
                .claim(JwtSupport.USER_ID, userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role))));
    }
}
