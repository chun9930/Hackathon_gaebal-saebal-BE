package com.mcm.privatecircle.product.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
	Long productId,
	String productCode,
	String name,
	String category,
	BigDecimal price,
	boolean recommendable
) {
}
