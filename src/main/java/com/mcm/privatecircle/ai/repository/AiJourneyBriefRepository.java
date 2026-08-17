package com.mcm.privatecircle.ai.repository;

import java.util.Optional;

import com.mcm.privatecircle.ai.entity.AiJourneyBrief;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiJourneyBriefRepository extends JpaRepository<AiJourneyBrief, Long> {

    Optional<AiJourneyBrief> findTopByCustomerIdAndVisitIdOrderByGeneratedAtDescIdDesc(
        Long customerId,
        Long visitId
    );

    Page<AiJourneyBrief> findByCustomerIdAndVisitStoreId(
        Long customerId,
        Long storeId,
        Pageable pageable
    );
}
