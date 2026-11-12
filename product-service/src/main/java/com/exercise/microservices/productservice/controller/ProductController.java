package com.exercise.microservices.productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.exercise.microservices.productservice.dto.CreateProductRequest;
import com.exercise.microservices.productservice.dto.ProductResponse;
import com.exercise.microservices.productservice.dto.QuantityRequest;
import com.exercise.microservices.productservice.dto.SearchProductsRequest;
import com.exercise.microservices.productservice.dto.UpdateProductRequest;
import com.exercise.microservices.productservice.service.ProductService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @PostMapping("/{id}/reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserve(@PathVariable Long id, @Valid @RequestBody QuantityRequest request) {
        productService.reserveStock(id, request.quantity());
    }

    @PostMapping("/{id}/reservations/releases")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long id, @Valid @RequestBody QuantityRequest request) {
        productService.releaseStock(id, request.quantity());
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAllProducts();
    }

    @GetMapping("/search")
    public List<ProductResponse> search(SearchProductsRequest request) {
        return productService.searchProducts(request);
    }
}
