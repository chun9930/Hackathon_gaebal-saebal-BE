package com.mcm.privatecircle.visit.dto;

import jakarta.validation.constraints.Size;

public record VisitRecordCreateRequest(
    @Size(max = 255) String visitPurpose,
    @Size(max = 5000) String content,
    @Size(max = 5000) String styleChangeNote,
    @Size(max = 5000) String cautionNote
) {
}
