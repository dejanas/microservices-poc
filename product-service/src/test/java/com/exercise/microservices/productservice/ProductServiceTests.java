package com.exercise.microservices.productservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.exercise.microservices.common.error.NotFoundException;
import com.exercise.microservices.productservice.dto.CreateProductRequest;
import com.exercise.microservices.productservice.dto.UpdateProductRequest;
import com.exercise.microservices.productservice.model.Product;
import com.exercise.microservices.productservice.repository.ProductRepository;
import com.exercise.microservices.productservice.service.ProductService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Notebook")
                .category("Stationery")
                .description("A5 lined")
                .stock(12)
                .build();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        productService.createProduct(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Notebook", captor.getValue().getName());
        assertEquals(12, captor.getValue().getStock());
    }

    @Test
    void shouldThrowWhenUpdatingMissingProduct() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Notebook")
                .category("Stationery")
                .description("A5 lined")
                .stock(12)
                .build();
        when(productRepository.findById(1234L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.updateProduct(1234L, request));
    }

    @Test
    void shouldUpdateProduct() {
        Product existing = Product.builder()
                .id(4L)
                .name("Old")
                .category("Old")
                .description("Old")
                .stock(1)
                .build();
        when(productRepository.findById(4L)).thenReturn(Optional.of(existing));

        productService.updateProduct(4L, UpdateProductRequest.builder()
                .name("Notebook")
                .category("Stationery")
                .description("A5 lined")
                .stock(12)
                .build());

        assertEquals("Notebook", existing.getName());
        assertEquals(12, existing.getStock());
    }

    @Test
    void shouldThrowWhenReadingMissingProduct() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getById(9L));
    }
}
