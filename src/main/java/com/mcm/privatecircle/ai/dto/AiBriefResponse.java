package com.mcm.privatecircle.ai.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.ai.entity.AiJourneyBrief;
import com.mcm.privatecircle.ai.entity.BriefStatus;

public record AiBriefResponse(
    Long briefId,
    Long customerId,
    Long visitId,
    Long requestedByCaId,
    String summary,
    String visitPurposeSummary,
    String interestSummary,
    String cautionSummary,
    String suggestedDirection,
    Integer sourceVisitCount,
    BriefStatus status,
    LocalDateTime generatedAt
) {

    public static AiBriefResponse from(AiJourneyBrief brief) {
        return new AiBriefResponse(
            brief.getId(),
            brief.getCustomer().getId(),
            brief.getVisit().getId(),
            brief.getRequestedByCa() == null ? null : brief.getRequestedByCa().getId(),
            brief.getSummary(),
            brief.getVisitPurposeSummary(),
            brief.getInterestSummary(),
            brief.getCautionSummary(),
            brief.getSuggestedDirection(),
            brief.getSourceVisitCount(),
            brief.getStatus(),
            brief.getGeneratedAt()
        );
    }
}
