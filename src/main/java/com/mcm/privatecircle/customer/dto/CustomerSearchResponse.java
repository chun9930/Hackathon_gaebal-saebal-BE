package com.mcm.privatecircle.customer.dto;

import java.time.LocalDateTime;

public record CustomerSearchResponse(
    Long customerId,
    String customerNo,
    String name,
    String phoneNumber,
    String profileImageUrl,
    String membershipGrade,
    LocalDateTime joinedAt
) {
}