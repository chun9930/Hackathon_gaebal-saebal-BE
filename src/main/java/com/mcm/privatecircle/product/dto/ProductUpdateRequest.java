package com.mcm.privatecircle.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
	@Size(max = 60)
	String productCode,

	@Size(max = 200)
	String name,

	@Size(max = 100)
	String category,

	@Size(max = 500)
	String imageUrl,

	BigDecimal price,

	@Size(max = 100)
	String dppId,

	Boolean recommendable
) {
}
