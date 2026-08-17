package com.mcm.privatecircle.visit.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.visit.entity.VisitRecord;

public record VisitRecordResponse(
    Long visitRecordId,
    Long visitId,
    Long customerId,
    Long caId,
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
            visitRecord.getCa().getId(),
            visitRecord.getVisitPurpose(),
            visitRecord.getContent(),
            visitRecord.getStyleChangeNote(),
            visitRecord.getCautionNote(),
            visitRecord.getCreatedAt()
        );
    }
}
