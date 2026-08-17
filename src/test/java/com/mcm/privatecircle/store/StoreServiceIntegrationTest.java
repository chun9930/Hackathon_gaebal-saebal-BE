package com.mcm.privatecircle.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;
import com.mcm.privatecircle.store.service.StoreService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StoreServiceIntegrationTest {

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreRepository storeRepository;

	@Test
	void getStoresReturnsSortedSummaryList() {
		storeRepository.save(new Store("Busan", "Busan"));
		storeRepository.save(new Store("Seoul", "Seoul"));

		var stores = storeService.getStores();

		assertThat(stores).hasSize(2);
		assertThat(stores.get(0).name()).isEqualTo("Busan");
		assertThat(stores.get(1).name()).isEqualTo("Seoul");
	}

	@Test
	void getStoreReturnsDetail() {
		Store saved = storeRepository.save(new Store("Gangnam", "Seoul"));

		var store = storeService.getStore(saved.getId());

		assertThat(store.storeId()).isEqualTo(saved.getId());
		assertThat(store.name()).isEqualTo("Gangnam");
		assertThat(store.location()).isEqualTo("Seoul");
		assertThat(store.createdAt()).isNotNull();
	}

	@Test
	void missingStoreThrowsNotFound() {
		assertThatThrownBy(() -> storeService.getStore(999L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.STORE_NOT_FOUND);
	}
}
