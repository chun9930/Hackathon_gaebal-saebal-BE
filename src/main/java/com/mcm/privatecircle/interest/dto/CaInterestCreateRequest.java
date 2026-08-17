package com.mcm.privatecircle.interest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CaInterestCreateRequest(
    @NotNull Long productId,
    @NotNull Long visitRecordId,
    @Size(max = 500) String memo
) {
}
