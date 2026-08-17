package com.mcm.privatecircle.ai.dto;

public record GeminiBriefResult(
    String summary,
    String visitPurposeSummary,
    String interestSummary,
    String cautionSummary,
    String suggestedDirection
) {
}
