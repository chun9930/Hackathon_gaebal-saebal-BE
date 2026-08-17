package com.mcm.privatecircle.store.dto;

import java.time.LocalDateTime;

public record StoreDetailResponse(
	Long storeId,
	String name,
	String location,
	LocalDateTime createdAt
) {
}
