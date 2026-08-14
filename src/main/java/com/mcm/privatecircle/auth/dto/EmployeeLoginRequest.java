package com.mcm.privatecircle.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmployeeLoginRequest(
	@NotBlank String loginId,
	@NotBlank String password
) {
}
