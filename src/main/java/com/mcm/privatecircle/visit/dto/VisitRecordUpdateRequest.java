package com.mcm.privatecircle.visit.dto;

import jakarta.validation.constraints.Size;

public record VisitRecordUpdateRequest(
    @Size(max = 255) String visitPurpose,
    @Size(max = 5000) String content,
    @Size(max = 5000) String styleChangeNote,
    @Size(max = 5000) String cautionNote
) {

    public boolean hasAnyField() {
        return visitPurpose != null
            || content != null
            || styleChangeNote != null
            || cautionNote != null;
    }
}
