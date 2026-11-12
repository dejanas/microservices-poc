package com.exercise.microservices.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.exercise.microservices.productservice.model.Product;
import com.exercise.microservices.productservice.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void searchesByNameDescriptionAndCategory() {
        productRepository.save(Product.builder().name("Red Apple").description("Fresh fruit").category("Fruit").stock(10).build());
        productRepository.save(Product.builder().name("Green Apple").description("Sour fruit").category("Fruit").stock(4).build());
        productRepository.save(Product.builder().name("Whole Milk").description("Dairy drink").category("Dairy").stock(6).build());

        assertEquals(2, productRepository.search("apple", null, null).size());
        assertEquals(1, productRepository.search(null, "sour", null).size());
        assertEquals(1, productRepository.search(null, null, "dairy").size());
        assertEquals(1, productRepository.search("apple", "fresh", "fruit").size());
        assertEquals(3, productRepository.search(null, null, null).size());
    }

    @Test
    void decreaseStockStopsAtZeroAndReleaseAddsItBack() {
        Product apple = productRepository.save(Product.builder()
                .name("Red Apple").description("Fresh fruit").category("Fruit").stock(5).build());

        assertEquals(1, productRepository.decreaseStock(apple.getId(), 3));
        assertEquals(2, productRepository.findById(apple.getId()).orElseThrow().getStock());
        assertEquals(0, productRepository.decreaseStock(apple.getId(), 3));
        assertEquals(2, productRepository.findById(apple.getId()).orElseThrow().getStock());
        assertEquals(1, productRepository.increaseStock(apple.getId(), 3));
        assertEquals(5, productRepository.findById(apple.getId()).orElseThrow().getStock());
    }
}
