package com.mcm.privatecircle.visit.repository;

import java.util.Optional;

import com.mcm.privatecircle.visit.entity.VisitRecord;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

    Optional<VisitRecord> findByVisitId(Long visitId);

    boolean existsByVisitId(Long visitId);

    Optional<VisitRecord> findByIdAndVisitStoreId(Long visitRecordId, Long storeId);
}
