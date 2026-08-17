package com.mcm.privatecircle.customer.dto;

import jakarta.validation.constraints.Size;

public record CustomerProfileUpdateRequest(
	@Size(min = 1, max = 100)
	String name,

	@Size(min = 10, max = 30)
	String phoneNumber,

	@Size(max = 500)
	String profileImageUrl,

	String stylePreferences
) {
}
