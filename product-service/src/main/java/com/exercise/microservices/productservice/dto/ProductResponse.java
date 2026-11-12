package com.exercise.microservices.productservice.dto;

import lombok.Builder;
import lombok.Getter;
import com.exercise.microservices.productservice.model.Product;

@Getter
@Builder
public class ProductResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final String category;
    private final Integer stock;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .stock(product.getStock())
                .build();
    }
}
