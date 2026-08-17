package com.mcm.privatecircle.interest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerInterestCreateRequest(
    @NotNull Long productId,
    @Size(max = 500) String memo
) {
}
