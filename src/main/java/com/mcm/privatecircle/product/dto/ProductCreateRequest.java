package com.mcm.privatecircle.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
	@NotBlank
	@Size(max = 60)
	String productCode,

	@NotBlank
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
