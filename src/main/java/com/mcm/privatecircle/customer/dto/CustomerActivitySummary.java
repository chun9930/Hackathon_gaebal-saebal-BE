package com.mcm.privatecircle.customer.dto;

import java.time.LocalDateTime;

public record CustomerActivitySummary(
    long visitCount,
    long stampCount,
    LocalDateTime lastVisitedAt
) {

    public static CustomerActivitySummary empty() {
        return new CustomerActivitySummary(0L, 0L, null);
    }
}
