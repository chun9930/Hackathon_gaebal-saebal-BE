package com.mcm.privatecircle.store.controller;

import java.util.List;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.store.dto.StoreDetailResponse;
import com.mcm.privatecircle.store.dto.StoreSummaryResponse;
import com.mcm.privatecircle.store.service.StoreService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

	private final StoreService storeService;

	public StoreController(StoreService storeService) {
		this.storeService = storeService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<StoreSummaryResponse>>> getStores() {
		return ResponseEntity.ok(ApiResponse.success(storeService.getStores()));
	}

	@GetMapping("/{storeId}")
	public ResponseEntity<ApiResponse<StoreDetailResponse>> getStore(@PathVariable Long storeId) {
		return ResponseEntity.ok(ApiResponse.success(storeService.getStore(storeId)));
	}
}
