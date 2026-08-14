package com.mcm.privatecircle.auth.dto;

import com.mcm.privatecircle.global.security.UserRole;

public record AuthTokenResponse(
	String accessToken,
	String tokenType,
	Long accountId,
	Long customerId,
	Long caId,
	Long storeId,
	UserRole role
) {
}
