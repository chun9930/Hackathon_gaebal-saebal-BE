package com.mcm.privatecircle.stamp.repository;

import com.mcm.privatecircle.stamp.entity.VisitStamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitStampRepository extends JpaRepository<VisitStamp, Long> {

    boolean existsByVisitId(Long visitId);

    @EntityGraph(attributePaths = {"visit", "issuedByCa"})
    Page<VisitStamp> findByCustomerIdAndVisitStoreId(
        Long customerId,
        Long storeId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"visit", "issuedByCa"})
    Page<VisitStamp> findByCustomerId(Long customerId, Pageable pageable);

    long countByCustomerId(Long customerId);
}
