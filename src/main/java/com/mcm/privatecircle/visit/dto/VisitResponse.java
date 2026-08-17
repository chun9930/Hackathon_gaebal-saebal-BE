package com.mcm.privatecircle.visit.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.visit.entity.Visit;

public record VisitResponse(
    Long visitId,
    Long customerId,
    Long storeId,
    LocalDateTime visitedAt
) {

    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
            visit.getId(),
            visit.getCustomer().getId(),
            visit.getStore().getId(),
            visit.getVisitedAt()
        );
    }
}
