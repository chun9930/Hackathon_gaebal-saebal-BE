package com.mcm.privatecircle.product.service;

import com.mcm.privatecircle.interest.repository.CustomerInterestProductRepository;
import com.mcm.privatecircle.purchase.repository.PurchaseHistoryRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DomainProductReferenceChecker implements ProductReferenceChecker {

    private final CustomerInterestProductRepository interestRepository;
    private final PurchaseHistoryRepository purchaseRepository;

    public DomainProductReferenceChecker(
        CustomerInterestProductRepository interestRepository,
        PurchaseHistoryRepository purchaseRepository
    ) {
        this.interestRepository = interestRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductInUse(Long productId) {
        return interestRepository.existsByProductId(productId)
            || purchaseRepository.existsByProductId(productId);
    }
}
