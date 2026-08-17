package com.mcm.privatecircle.store.dto;

public record StoreSummaryResponse(
	Long storeId,
	String name,
	String location
) {
}
