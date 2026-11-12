package com.exercise.microservices.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.common.security.JwtSupport;
import com.exercise.microservices.orderservice.service.ProductClient;

import java.time.Instant;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductClient productClient;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void onlyTheOwnerOrAnAdminCanViewAnOrder() throws Exception {
        String body = """
                {"items":[{"productId":5,"quantity":2}]}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", token(10L, "ana", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderUrl = "/api/orders/" + response.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mockMvc.perform(get(orderUrl).header("Authorization", token(11L, "marko", "USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(orderUrl).header("Authorization", token(10L, "ana", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10));

        mockMvc.perform(get(orderUrl).header("Authorization", token(1L, "admin", "ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/me").header("Authorization", token(10L, "ana", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(10));

        mockMvc.perform(get("/api/orders/users/10").header("Authorization", token(11L, "marko", "USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/orders/users/10").header("Authorization", token(1L, "admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(10));
    }

    private String token(Long userId, String username, String role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim(JwtSupport.USER_ID, userId)
                .claim(JwtSupport.ROLES, List.of(role))
                .build();
        return "Bearer " + JwtSupport.encode(JwtSupport.encoder(secret), claims);
    }
}
