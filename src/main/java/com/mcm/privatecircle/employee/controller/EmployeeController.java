package com.mcm.privatecircle.employee.controller;

import com.mcm.privatecircle.employee.dto.EmployeeProfileResponse;
import com.mcm.privatecircle.employee.service.EmployeeService;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<EmployeeProfileResponse>> getMyProfile(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ResponseEntity.ok(ApiResponse.success(employeeService.getMyProfile(authenticatedUser)));
	}
}
