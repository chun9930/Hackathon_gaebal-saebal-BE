package com.mcm.privatecircle.visit.controller;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.visit.dto.VisitRecordCreateRequest;
import com.mcm.privatecircle.visit.dto.VisitRecordResponse;
import com.mcm.privatecircle.visit.dto.VisitRecordUpdateRequest;
import com.mcm.privatecircle.visit.service.VisitRecordService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VisitRecordController {

    private final VisitRecordService visitRecordService;

    public VisitRecordController(VisitRecordService visitRecordService) {
        this.visitRecordService = visitRecordService;
    }

    @PostMapping("/visits/{visitId}/records")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<VisitRecordResponse>> createVisitRecord(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long visitId,
        @Valid @RequestBody VisitRecordCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                visitRecordService.createVisitRecord(authenticatedUser, visitId, request)
            ));
    }

    @GetMapping("/visits/{visitId}/records")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<VisitRecordResponse>> getVisitRecord(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long visitId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitRecordService.getVisitRecord(authenticatedUser, visitId)
        ));
    }

    @PatchMapping("/visit-records/{visitRecordId}")
    @PreAuthorize("hasRole(''CA'')")
    public ResponseEntity<ApiResponse<VisitRecordResponse>> updateVisitRecord(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long visitRecordId,
        @Valid @RequestBody VisitRecordUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            visitRecordService.updateVisitRecord(authenticatedUser, visitRecordId, request)
        ));
    }
}
