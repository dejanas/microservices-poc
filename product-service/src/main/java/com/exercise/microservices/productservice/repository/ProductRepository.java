package com.exercise.microservices.productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.exercise.microservices.productservice.model.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p
            WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%')))
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
            """)
    List<Product> search(
            @Param("name") String name,
            @Param("description") String description,
            @Param("category") String category);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock - :quantity
            WHERE p.id = :id AND p.stock >= :quantity
            """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock + :quantity
            WHERE p.id = :id
            """)
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
