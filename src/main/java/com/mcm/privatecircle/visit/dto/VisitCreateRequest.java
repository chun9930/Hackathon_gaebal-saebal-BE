package com.mcm.privatecircle.visit.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VisitCreateRequest(
    @NotNull @Positive Long customerId,
    @NotNull LocalDateTime visitedAt
) {
}
