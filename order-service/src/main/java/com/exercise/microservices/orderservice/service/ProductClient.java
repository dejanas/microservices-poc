package com.exercise.microservices.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.exercise.microservices.common.error.BadRequestException;
import com.exercise.microservices.common.error.ServiceUnavailableException;
import com.exercise.microservices.common.security.CurrentUser;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public void reserve(Long productId, int quantity) {
        changeStock(productId, quantity, "http://product-service/api/products/{id}/reservations");
    }

    public void release(Long productId, int quantity) {
        changeStock(productId, quantity, "http://product-service/api/products/{id}/reservations/releases");
    }

    private void changeStock(Long productId, int quantity, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(CurrentUser.tokenValue());
        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("quantity", quantity), headers),
                    Void.class,
                    productId);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new BadRequestException("Product " + productId + " does not exist");
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new BadRequestException("Product " + productId + " does not have enough stock");
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Could not verify product availability", exception);
        }
    }
}
