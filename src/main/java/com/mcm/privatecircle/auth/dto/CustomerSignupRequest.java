package com.mcm.privatecircle.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSignupRequest(
	@NotBlank
	@Size(min = 4, max = 100)
	String loginId,

	@NotBlank
	@Size(min = 8, max = 64)
	String password,

	@NotBlank
	@Size(min = 1, max = 100)
	String name,

	@NotBlank
	@Size(min = 10, max = 30)
	String phoneNumber
) {
}
