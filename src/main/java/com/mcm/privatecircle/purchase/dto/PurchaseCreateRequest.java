package com.mcm.privatecircle.purchase.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseCreateRequest(
    @NotNull Long customerId,
    @NotNull Long productId,
    Long visitId,
    @NotNull @Min(1) Integer quantity,
    @NotNull LocalDateTime purchasedAt
) {
}
