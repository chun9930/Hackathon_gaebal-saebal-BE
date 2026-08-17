package com.mcm.privatecircle.stamp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VisitStampCreateRequest(
    @NotBlank @Size(max = 30) String stampType
) {
}
