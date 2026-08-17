package com.mcm.privatecircle.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.mcm.privatecircle.account.entity.EmployeeAccount;
import com.mcm.privatecircle.account.repository.EmployeeAccountRepository;
import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.UserRole;
import com.mcm.privatecircle.product.dto.ProductCreateRequest;
import com.mcm.privatecircle.product.dto.ProductUpdateRequest;
import com.mcm.privatecircle.product.repository.ProductRepository;
import com.mcm.privatecircle.product.service.ProductService;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private EmployeeAccountRepository employeeAccountRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void productCrudWorksForCa() {
		Store store = storeRepository.save(new Store("Gangnam", "Seoul"));
		EmployeeAccount employeeAccount = employeeAccountRepository.save(
			new EmployeeAccount("employee-p1", passwordEncoder.encode("password123!"))
		);
		AuthenticatedUser caUser = AuthenticatedUser.ca(employeeAccount.getId(), 1L, store.getId());

		var created = productService.createProduct(
			caUser,
			new ProductCreateRequest(
				"PROD-001",
				"Bag",
				"Accessory",
				"https://img.example.com/bag.png",
				new BigDecimal("120000.00"),
				"DPP-1",
				true
			)
		);

		assertThat(created.productCode()).isEqualTo("PROD-001");
		assertThat(created.recommendable()).isTrue();

		var list = productService.getProducts();
		assertThat(list).hasSize(1);
		assertThat(list.get(0).name()).isEqualTo("Bag");

		var updated = productService.updateProduct(
			caUser,
			created.productId(),
			new ProductUpdateRequest(
				"PROD-001-NEW",
				"Bag Updated",
				"Accessory",
				"https://img.example.com/bag2.png",
				new BigDecimal("130000.00"),
				"DPP-2",
				false
			)
		);

		assertThat(updated.productCode()).isEqualTo("PROD-001-NEW");
		assertThat(updated.recommendable()).isFalse();
		assertThat(updated.name()).isEqualTo("Bag Updated");

		productService.deleteProduct(caUser, created.productId());
		assertThat(productRepository.findById(created.productId())).isEmpty();
	}

	@Test
	void productLookupReturnsNotFoundForMissingProduct() {
		assertThatThrownBy(() -> productService.getProduct(999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
	}

	@Test
	void nonCaCannotManageProducts() {
		assertThatThrownBy(() -> productService.createProduct(
			AuthenticatedUser.customer(10L, 20L),
			new ProductCreateRequest("PROD-002", "Bag", "Accessory", null, null, null, null)
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void duplicateProductCodeIsRejected() {
		Store store = storeRepository.save(new Store("Gangnam", "Seoul"));
		EmployeeAccount employeeAccount = employeeAccountRepository.save(
			new EmployeeAccount("employee-p2", passwordEncoder.encode("password123!"))
		);
		AuthenticatedUser caUser = AuthenticatedUser.ca(employeeAccount.getId(), 2L, store.getId());

		productService.createProduct(
			caUser,
			new ProductCreateRequest("PROD-003", "Bag", "Accessory", null, null, null, null)
		);

		assertThatThrownBy(() -> productService.createProduct(
			caUser,
			new ProductCreateRequest("PROD-003", "Bag2", "Accessory", null, null, null, null)
		))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
