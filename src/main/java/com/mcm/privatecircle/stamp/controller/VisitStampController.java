package com.mcm.privatecircle.stamp.controller;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.stamp.dto.VisitStampCreateRequest;
import com.mcm.privatecircle.stamp.dto.VisitStampResponse;
import com.mcm.privatecircle.stamp.service.VisitStampService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VisitStampController {

    private final VisitStampService visitStampService;

    public VisitStampController(VisitStampService visitStampService) {
        this.visitStampService = visitStampService;
    }

    @PostMapping("/visits/{visitId}/stamps")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<VisitStampResponse>> issueStamp(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long visitId,
        @Valid @RequestBody VisitStampCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(visitStampService.issueStamp(
                authenticatedUser, visitId, request
            )));
    }

    @GetMapping("/customers/{customerId}/stamps")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<PageResponse<VisitStampResponse>>> getCustomerStamps(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitStampService.getCustomerStamps(authenticatedUser, customerId, page, size)
        ));
    }

    @GetMapping("/customers/me/stamps")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<VisitStampResponse>>> getMyStamps(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitStampService.getMyStamps(authenticatedUser, page, size)
        ));
    }
}
