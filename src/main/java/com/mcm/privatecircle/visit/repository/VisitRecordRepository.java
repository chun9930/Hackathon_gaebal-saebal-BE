package com.mcm.privatecircle.visit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.mcm.privatecircle.visit.entity.VisitRecord;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    Optional<VisitRecord> findByVisitId(Long visitId);

    boolean existsByVisitId(Long visitId);

    Optional<VisitRecord> findByIdAndVisitStoreId(Long visitRecordId, Long storeId);

    @EntityGraph(attributePaths = "visit")
    List<VisitRecord> findByCustomerIdAndVisitStoreIdAndVisitVisitedAtLessThan(
        Long customerId,
        Long storeId,
        LocalDateTime visitedAt,
        Pageable pageable
    );
}
