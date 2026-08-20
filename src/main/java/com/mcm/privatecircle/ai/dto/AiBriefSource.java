package com.mcm.privatecircle.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.interest.entity.InterestSourceType;

public record AiBriefSource(
    CustomerProfile customer,
    VisitRecordSource currentVisitRecord,
    List<VisitRecordSource> visitRecords,
    List<InterestProductSource> interestProducts,
    List<PurchaseSource> purchases,
    int sourceVisitCount
) {

    public AiBriefSource {
        visitRecords = List.copyOf(visitRecords);
        interestProducts = List.copyOf(interestProducts);
        purchases = List.copyOf(purchases);
    }

    public record CustomerProfile(
        String membershipGrade,
        String stylePreferences
    ) {
    }

    public record VisitRecordSource(
        LocalDateTime visitedAt,
        String visitPurpose,
        String content,
        String styleChangeNote,
        String cautionNote
    ) {
    }

    public record InterestProductSource(
        String productName,
        String category,
        InterestSourceType sourceType,
        String memo,
        LocalDateTime savedAt
    ) {
    }

    public record PurchaseSource(
        String productName,
        String category,
        Integer quantity,
        LocalDateTime purchasedAt
    ) {
    }
}
