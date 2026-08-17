package com.mcm.privatecircle.visit.controller;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.dto.VisitCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitResponse;
import com.mcm.privatecircle.visit.service.VisitService;

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
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping("/visits")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<VisitResponse>> createVisit(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody VisitCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(visitService.createVisit(authenticatedUser, request)));
    }

    @GetMapping("/customers/{customerId}/visits")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<PageResponse<VisitResponse>>> getCustomerVisits(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitService.getCustomerVisits(authenticatedUser, customerId, page, size)
        ));
    }

    @GetMapping("/visits/{visitId}")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<VisitResponse>> getVisit(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long visitId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitService.getVisit(authenticatedUser, visitId)
        ));
    }
}
