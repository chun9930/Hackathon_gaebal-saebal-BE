package com.mcm.privatecircle.purchase.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.purchase.entity.PurchaseHistory;

public record PurchaseResponse(
    Long purchaseId,
    Long customerId,
    Long productId,
    String productName,
    String category,
    Long storeId,
    String storeName,
    Long visitId,
    Integer quantity,
    LocalDateTime purchasedAt
) {

    public static PurchaseResponse from(PurchaseHistory purchase) {
        return new PurchaseResponse(
            purchase.getId(),
            purchase.getCustomer().getId(),
            purchase.getProduct().getId(),
            purchase.getProduct().getName(),
            purchase.getProduct().getCategory(),
            purchase.getStore().getId(),
            purchase.getStore().getName(),
            purchase.getVisit() == null ? null : purchase.getVisit().getId(),
            purchase.getQuantity(),
            purchase.getPurchasedAt()
        );
    }
}
