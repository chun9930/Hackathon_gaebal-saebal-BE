package com.mcm.privatecircle.employee.dto;

import java.time.LocalDateTime;

public record EmployeeProfileResponse(
	Long caId,
	Long storeId,
	String storeName,
	String name,
	Long accountId,
	String loginId,
	LocalDateTime createdAt
) {
}
