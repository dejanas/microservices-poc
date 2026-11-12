package com.exercise.microservices.productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.exercise.microservices.common.error.BadRequestException;
import com.exercise.microservices.common.error.NotFoundException;
import com.exercise.microservices.productservice.dto.CreateProductRequest;
import com.exercise.microservices.productservice.dto.ProductResponse;
import com.exercise.microservices.productservice.dto.SearchProductsRequest;
import com.exercise.microservices.productservice.dto.UpdateProductRequest;
import com.exercise.microservices.productservice.model.Product;
import com.exercise.microservices.productservice.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .stock(request.getStock())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setStock(request.getStock());
        log.info("Product {} is updated", product.getId());
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product " + id + " was not found");
        }
        productRepository.deleteById(id);
        log.info("Product {} is deleted", id);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public void reserveStock(Long id, int quantity) {
        if (productRepository.decreaseStock(id, quantity) == 1) {
            return;
        }
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product " + id + " was not found");
        }
        throw new BadRequestException("Product " + id + " does not have enough stock");
    }

    @Transactional
    public void releaseStock(Long id, int quantity) {
        if (productRepository.increaseStock(id, quantity) == 0) {
            throw new NotFoundException("Product " + id + " was not found");
        }
    }

    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
    }

    public List<ProductResponse> searchProducts(SearchProductsRequest request) {
        SearchProductsRequest safe = request == null ? new SearchProductsRequest() : request;
        return productRepository.search(
                        blankToNull(safe.getName()),
                        blankToNull(safe.getDescription()),
                        blankToNull(safe.getCategory()))
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
