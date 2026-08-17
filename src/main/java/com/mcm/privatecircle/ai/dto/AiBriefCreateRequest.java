package com.mcm.privatecircle.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiBriefCreateRequest(
    @NotNull Long visitId
) {
}
