package com.mcm.privatecircle.interest.dto;

import java.time.LocalDateTime;

import com.mcm.privatecircle.interest.entity.CustomerInterestProduct;
import com.mcm.privatecircle.interest.entity.InterestSourceType;

public record InterestProductResponse(
    Long interestProductId,
    Long customerId,
    Long productId,
    String productName,
    String category,
    InterestSourceType sourceType,
    Long visitRecordId,
    String memo,
    LocalDateTime savedAt
) {

    public static InterestProductResponse from(CustomerInterestProduct interest) {
        return new InterestProductResponse(
            interest.getId(),
            interest.getCustomer().getId(),
            interest.getProduct().getId(),
            interest.getProduct().getName(),
            interest.getProduct().getCategory(),
            interest.getSourceType(),
            interest.getVisitRecord() == null ? null : interest.getVisitRecord().getId(),
            interest.getMemo(),
            interest.getSavedAt()
        );
    }
}
