package com.mcm.privatecircle.customer.controller;

import com.mcm.privatecircle.customer.dto.CustomerProfileResponse;
import com.mcm.privatecircle.customer.dto.CustomerProfileUpdateRequest;
import com.mcm.privatecircle.customer.dto.CustomerSearchResponse;
import com.mcm.privatecircle.customer.service.CustomerService;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ResponseEntity.ok(ApiResponse.success(customerService.getMyProfile(authenticatedUser)));
	}

	@PatchMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMyProfile(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody CustomerProfileUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(customerService.updateMyProfile(authenticatedUser, request)));
	}

    @GetMapping("/search")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerSearchResponse>>> searchCustomers(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            customerService.searchCustomers(authenticatedUser, keyword, page, size)
        ));
    }

	@GetMapping("/{customerId}")
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCustomerDetail(@PathVariable Long customerId) {
		return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerDetail(customerId)));
	}

	@GetMapping("/by-qr/{qrToken}")
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCustomerByQrToken(@PathVariable String qrToken) {
		return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerByQrToken(qrToken)));
	}
}