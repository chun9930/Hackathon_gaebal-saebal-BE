package com.mcm.privatecircle.visit.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.visit.entity.Visit;

public record VisitResponse(
    Long visitId,
    Long customerId,
    String customerName,
    Long storeId,
    String storeName,
    LocalDateTime visitedAt
) {

    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
            visit.getId(),
            visit.getCustomer().getId(),
            visit.getCustomer().getName(),
            visit.getStore().getId(),
            visit.getStore().getName(),
            visit.getVisitedAt()
        );
    }
}