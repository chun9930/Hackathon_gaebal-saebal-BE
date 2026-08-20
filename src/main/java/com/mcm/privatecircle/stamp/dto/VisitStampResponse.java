package com.mcm.privatecircle.stamp.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.stamp.entity.VisitStamp;

public record VisitStampResponse(
    Long stampId,
    Long visitId,
    Long customerId,
    String customerName,
    Long storeId,
    String storeName,
    String stampImageUrl,
    Long issuedByCaId,
    String issuedByCaName,
    String stampType,
    LocalDateTime issuedAt,
    LocalDateTime visitedAt
) {

    public static VisitStampResponse from(VisitStamp stamp) {
        return new VisitStampResponse(
            stamp.getId(),
            stamp.getVisit().getId(),
            stamp.getCustomer().getId(),
            stamp.getCustomer().getName(),
            stamp.getVisit().getStore().getId(),
            stamp.getVisit().getStore().getName(),
            StampImageResolver.resolve(stamp.getVisit().getStore().getName()),
            stamp.getIssuedByCa().getId(),
            stamp.getIssuedByCa().getName(),
            stamp.getStampType(),
            stamp.getIssuedAt(),
            stamp.getVisit().getVisitedAt()
        );
    }
}
