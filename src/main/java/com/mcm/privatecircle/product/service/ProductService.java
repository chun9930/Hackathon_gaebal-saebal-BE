package com.mcm.privatecircle.product.service;

import java.util.List;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.product.dto.ProductCreateRequest;
import com.mcm.privatecircle.product.dto.ProductResponse;
import com.mcm.privatecircle.product.dto.ProductSummaryResponse;
import com.mcm.privatecircle.product.dto.ProductUpdateRequest;
import com.mcm.privatecircle.product.entity.Product;
import com.mcm.privatecircle.product.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public List<ProductSummaryResponse> getProducts() {
		return productRepository.findAll().stream()
			.map(this::toSummaryResponse)
			.toList();
	}

	public ProductResponse getProduct(Long productId) {
		return toResponse(findProduct(productId));
	}

	@Transactional
	public ProductResponse createProduct(AuthenticatedUser authenticatedUser, ProductCreateRequest request) {
		assertCa(authenticatedUser);
		if (productRepository.existsByProductCode(request.productCode())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		Product product = productRepository.save(new Product(
			request.productCode(),
			request.name(),
			request.category(),
			request.imageUrl(),
			request.price(),
			request.dppId(),
			request.recommendable() == null || request.recommendable()
		));
		return toResponse(product);
	}

	@Transactional
	public ProductResponse updateProduct(AuthenticatedUser authenticatedUser, Long productId, ProductUpdateRequest request) {
		assertCa(authenticatedUser);
		Product product = findProduct(productId);
		if (request.productCode() != null
			&& !request.productCode().equals(product.getProductCode())
			&& productRepository.existsByProductCode(request.productCode())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		product.update(
			request.productCode(),
			request.name(),
			request.category(),
			request.imageUrl(),
			request.price(),
			request.dppId(),
			request.recommendable()
		);
		return toResponse(product);
	}

	@Transactional
	public void deleteProduct(AuthenticatedUser authenticatedUser, Long productId) {
		assertCa(authenticatedUser);
		Product product = findProduct(productId);
		productRepository.delete(product);
	}

	private void assertCa(AuthenticatedUser authenticatedUser) {
		if (authenticatedUser == null || authenticatedUser.getRole() == null || authenticatedUser.getRole() != com.mcm.privatecircle.global.security.UserRole.CA) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
	}

	private Product findProduct(Long productId) {
		return productRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private ProductSummaryResponse toSummaryResponse(Product product) {
		return new ProductSummaryResponse(
			product.getId(),
			product.getProductCode(),
			product.getName(),
			product.getCategory(),
			product.getPrice(),
			product.isRecommendable()
		);
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
			product.getId(),
			product.getProductCode(),
			product.getName(),
			product.getCategory(),
			product.getImageUrl(),
			product.getPrice(),
			product.getDppId(),
			product.isRecommendable(),
			product.getCreatedAt()
		);
	}
}
