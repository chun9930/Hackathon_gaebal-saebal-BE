package com.mcm.privatecircle.customer.dto;

import java.time.LocalDateTime;

public record CustomerProfileResponse(
	Long customerId,
	String name,
	String phoneNumber,
	String profileImageUrl,
	String membershipGrade,
	String stylePreferences,
	Long visitCount,
	Long stampCount,
	LocalDateTime lastVisitedAt,
	LocalDateTime joinedAt
) {
}
