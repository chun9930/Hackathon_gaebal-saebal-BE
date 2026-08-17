package com.mcm.privatecircle.purchase.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.mcm.privatecircle.purchase.entity.PurchaseHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {

    @EntityGraph(attributePaths = {"product", "store", "visit"})
    Page<PurchaseHistory> findByCustomerIdAndStoreId(
        Long customerId,
        Long storeId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "product")
    List<PurchaseHistory> findByCustomerIdAndStoreIdAndPurchasedAtLessThan(
        Long customerId,
        Long storeId,
        LocalDateTime purchasedAt,
        Pageable pageable
    );

    boolean existsByProductId(Long productId);
}
