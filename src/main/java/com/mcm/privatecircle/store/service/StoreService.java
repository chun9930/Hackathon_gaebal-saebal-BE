package com.mcm.privatecircle.store.service;

import java.util.Comparator;
import java.util.List;

import com.mcm.privatecircle.store.dto.StoreDetailResponse;
import com.mcm.privatecircle.store.dto.StoreSummaryResponse;
import com.mcm.privatecircle.store.entity.Store;
import com.mcm.privatecircle.store.repository.StoreRepository;

import com.mcm.privatecircle.global.exception.BusinessException;
import com.mcm.privatecircle.global.exception.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoreService {

	private final StoreRepository storeRepository;

	public StoreService(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	public List<StoreSummaryResponse> getStores() {
		return storeRepository.findAll().stream()
			.sorted(Comparator.comparing(Store::getId))
			.map(store -> new StoreSummaryResponse(
				store.getId(),
				store.getName(),
				store.getLocation()
			))
			.toList();
	}

	public StoreDetailResponse getStore(Long storeId) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

		return new StoreDetailResponse(
			store.getId(),
			store.getName(),
			store.getLocation(),
			store.getCreatedAt()
		);
	}
}
