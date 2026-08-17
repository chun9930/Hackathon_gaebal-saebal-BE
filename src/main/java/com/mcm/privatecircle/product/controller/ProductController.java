package com.mcm.privatecircle.product.controller;

import java.util.List;

import com.mcm.privatecircle.global.response.ApiResponse;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.product.dto.ProductCreateRequest;
import com.mcm.privatecircle.product.dto.ProductResponse;
import com.mcm.privatecircle.product.dto.ProductSummaryResponse;
import com.mcm.privatecircle.product.dto.ProductUpdateRequest;
import com.mcm.privatecircle.product.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('CUSTOMER', 'CA')")
	public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getProducts() {
		return ResponseEntity.ok(ApiResponse.success(productService.getProducts()));
	}

	@GetMapping("/{productId}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'CA')")
	public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
	}

	@PostMapping
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody ProductCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(productService.createProduct(authenticatedUser, request)));
	}

	@PatchMapping("/{productId}")
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long productId,
		@Valid @RequestBody ProductUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(authenticatedUser, productId, request)));
	}

	@DeleteMapping("/{productId}")
	@PreAuthorize("hasRole('CA')")
	public ResponseEntity<ApiResponse<Void>> deleteProduct(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable Long productId
	) {
		productService.deleteProduct(authenticatedUser, productId);
		return ResponseEntity.ok(ApiResponse.success(null, "상품이 삭제되었습니다."));
	}
}
