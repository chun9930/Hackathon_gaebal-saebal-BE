package com.mcm.privatecircle.ai.controller;

import com.mcm.privatecircle.ai.dto.AiBriefResponse;
import com.mcm.privatecircle.ai.service.AiBriefService;
import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.response.PageResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/ai-briefs")
public class AiBriefController {

    private final AiBriefService service;

    public AiBriefController(AiBriefService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<AiBriefResponse>> getLatest(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @RequestParam Long visitId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            service.getLatest(authenticatedUser, customerId, visitId)
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<ApiResponse<PageResponse<AiBriefResponse>>> getHistory(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            service.getHistory(authenticatedUser, customerId, page, size)
        ));
    }
}
