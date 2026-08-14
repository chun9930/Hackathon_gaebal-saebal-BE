package com.mcm.privatecircle.product.repository;

import java.util.Optional;

import com.mcm.privatecircle.product.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findByProductCode(String productCode);

	boolean existsByProductCode(String productCode);
}
