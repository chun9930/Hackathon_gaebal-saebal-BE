package com.mcm.privatecircle.visit.repository;

import java.util.Optional;

import com.mcm.privatecircle.visit.entity.Visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    Optional<Visit> findByIdAndStoreId(Long visitId, Long storeId);

    Page<Visit> findByCustomerIdAndStoreId(Long customerId, Long storeId, Pageable pageable);

    long countByCustomerId(Long customerId);

    Optional<Visit> findTopByCustomerIdOrderByVisitedAtDescIdDesc(Long customerId);
}
