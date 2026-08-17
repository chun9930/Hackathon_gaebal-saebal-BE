package com.mcm.privatecircle.stamp.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.stamp.entity.VisitStamp;

public record VisitStampResponse(
    Long stampId,
    Long visitId,
    Long customerId,
    Long issuedByCaId,
    String stampType,
    LocalDateTime issuedAt
) {

    public static VisitStampResponse from(VisitStamp stamp) {
        return new VisitStampResponse(
            stamp.getId(),
            stamp.getVisit().getId(),
            stamp.getCustomer().getId(),
            stamp.getIssuedByCa().getId(),
            stamp.getStampType(),
            stamp.getIssuedAt()
        );
    }
}
