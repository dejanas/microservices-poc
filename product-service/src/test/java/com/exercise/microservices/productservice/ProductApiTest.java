package com.exercise.microservices.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.common.security.JwtSupport;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class ProductApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void usersCanReadProductsButOnlyAdminsCanChangeThem() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/products").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        String body = """
                {"name":"Notebook","description":"A5 lined","category":"Stationery","stock":12}
                """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", token(2L, "ana", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", token(1L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Notebook"))
                .andExpect(jsonPath("$.stock").value(12));

        mockMvc.perform(get("/api/products/search")
                        .header("Authorization", token(2L, "ana", "USER"))
                        .param("name", "note")
                        .param("category", "stationery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("Stationery"));

        mockMvc.perform(post("/api/products/1/reservations")
                        .header("Authorization", token(2L, "ana", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":2}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/1").header("Authorization", token(2L, "ana", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        mockMvc.perform(post("/api/products/1/reservations")
                        .header("Authorization", token(2L, "ana", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":11}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/products/1")
                        .header("Authorization", token(2L, "ana", "USER")))
                .andExpect(status().isForbidden());
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
