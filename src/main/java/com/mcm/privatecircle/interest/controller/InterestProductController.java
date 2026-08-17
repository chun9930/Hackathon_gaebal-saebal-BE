package com.mcm.privatecircle.interest.controller;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.interest.dto.CaInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.CustomerInterestCreateRequest;
import com.mcm.privatecircle.interest.dto.InterestProductResponse;
import com.mcm.privatecircle.interest.service.InterestProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InterestProductController {

    private final InterestProductService interestProductService;

    public InterestProductController(InterestProductService interestProductService) {
        this.interestProductService = interestProductService;
    }

    @PostMapping("/customers/me/interest-products")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<InterestProductResponse>> createMyInterestProduct(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody CustomerInterestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                interestProductService.createCustomerInterest(authenticatedUser, request)
            ));
    }

    @GetMapping("/customers/me/interest-products")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<InterestProductResponse>>> getMyInterestProducts(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            interestProductService.getMyInterestProducts(authenticatedUser, page, size)
        ));
    }

    @PostMapping("/customers/{customerId}/interest-products")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<InterestProductResponse>> createCaInterestProduct(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @Valid @RequestBody CaInterestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                interestProductService.createCaInterest(authenticatedUser, customerId, request)
            ));
    }

    @GetMapping("/customers/{customerId}/interest-products")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<PageResponse<InterestProductResponse>>> getCustomerInterestProducts(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            interestProductService.getCustomerInterestProducts(
                authenticatedUser, customerId, page, size
            )
        ));
    }

    @DeleteMapping("/interest-products/{interestProductId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CA')")
    public ResponseEntity<ApiResponse<Void>> deleteInterestProduct(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long interestProductId
    ) {
        interestProductService.deleteInterestProduct(authenticatedUser, interestProductId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
