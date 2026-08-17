package com.mcm.privatecircle.visit.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.visit.entity.VisitRecord;

public record VisitRecordResponse(
    Long visitRecordId,
    Long visitId,
    Long customerId,
    String customerName,
    Long caId,
    String caName,
    Long storeId,
    String storeName,
    LocalDateTime visitedAt,
    String visitPurpose,
    String content,
    String styleChangeNote,
    String cautionNote,
    LocalDateTime createdAt
) {

    public static VisitRecordResponse from(VisitRecord visitRecord) {
        return new VisitRecordResponse(
            visitRecord.getId(),
            visitRecord.getVisit().getId(),
            visitRecord.getCustomer().getId(),
            visitRecord.getCustomer().getName(),
            visitRecord.getCa().getId(),
            visitRecord.getCa().getName(),
            visitRecord.getVisit().getStore().getId(),
            visitRecord.getVisit().getStore().getName(),
            visitRecord.getVisit().getVisitedAt(),
            visitRecord.getVisitPurpose(),
            visitRecord.getContent(),
            visitRecord.getStyleChangeNote(),
            visitRecord.getCautionNote(),
            visitRecord.getCreatedAt()
        );
    }
}