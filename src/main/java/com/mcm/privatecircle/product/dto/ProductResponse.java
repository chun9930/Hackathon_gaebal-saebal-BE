package com.mcm.privatecircle.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
	Long productId,
	String productCode,
	String name,
	String category,
	String imageUrl,
	BigDecimal price,
	String dppId,
	boolean recommendable,
	LocalDateTime createdAt
) {
}
